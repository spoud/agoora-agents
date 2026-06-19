package io.spoud.agoora.agents.profiler.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.spoud.agoora.agents.profiler.config.ProfilerConfig;
import io.spoud.agoora.agents.profiler.model.ProfilingResult;
import io.spoud.sdm.profiler.domain.v1alpha1.Meta;
import io.spoud.sdm.profiler.domain.v1alpha1.ProfilerError;
import io.spoud.sdm.profiler.service.v1alpha1.InspectionDataStreamResponse;
import io.spoud.sdm.profiler.service.v1alpha1.InspectionRequest;
import io.spoud.sdm.profiler.service.v1alpha1.ProfileDataStreamResponse;
import io.spoud.sdm.profiler.service.v1alpha1.ProfileRequest;
import io.spoud.sdm.profiler.service.v1alpha1.Profiler;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Singleton
@RequiredArgsConstructor
@io.quarkus.grpc.GrpcService
public class ProfilerGrpcService implements Profiler {

    // Match Python service chunk size limit (gRPC 4 MB message limit)
    private static final int CHUNK_SIZE = 4_194_150;

    private final ProfilerConfig config;
    private final ObjectMapper objectMapper;
    private final JsonSchemaInferenceService schemaService;
    private final DataProfilerService profilerService;

    @Override
    public Multi<ProfileDataStreamResponse> profileDataStream(Multi<ProfileRequest> requests) {
        return requests
                .collect().asList()
                .onItem().transformToMulti(this::buildResponseStream);
    }

    @Override
    public Uni<InspectionDataStreamResponse> inspectQuality(Multi<InspectionRequest> request) {
        return Uni.createFrom().failure(new StatusRuntimeException(Status.UNIMPLEMENTED));
    }

    private Multi<ProfileDataStreamResponse> buildResponseStream(List<ProfileRequest> reqs) {
        if (reqs.isEmpty()) {
            return Multi.createFrom().item(
                    errorMeta("", ProfilerError.Type.NO_DATA, "No data provided"));
        }

        // Parse JSON records; fail fast on first decode error
        String requestId = reqs.get(0).getRequestId();
        List<Map<String, Object>> records = new ArrayList<>(reqs.size());
        List<String> rawJsonStrings = new ArrayList<>(reqs.size());
        for (ProfileRequest req : reqs) {
            String jsonData = req.getJsonData();
            if (jsonData == null || jsonData.isBlank()) {
                return Multi.createFrom().item(
                        errorMeta(requestId, ProfilerError.Type.UNKNOWN_ENCODING, "Empty json_data field"));
            }
            try {
                Map<String, Object> parsed = objectMapper.readValue(
                        jsonData, new TypeReference<>() {});
                records.add(parsed);
                rawJsonStrings.add(jsonData);
            } catch (Exception e) {
                LOG.warn("JSON decode error for request {}: {}", requestId, e.getMessage());
                return Multi.createFrom().item(
                        errorMeta(requestId, ProfilerError.Type.UNKNOWN_ENCODING, e.getMessage()));
            }
        }

        final String finalRequestId = requestId;
        final List<Map<String, Object>> finalRecords = records;
        final List<String> finalRawJsonStrings = rawJsonStrings;

        return Uni.createFrom()
                .item(() -> {
                    try {
                        return runProfiling(finalRequestId, finalRecords, finalRawJsonStrings);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .ifNoItem().after(Duration.ofSeconds(config.timeout()))
                .failWith(() -> new TimeoutException(
                        "Profiling timed out after " + config.timeout() + "s"))
                .onFailure().recoverWithItem(t -> {
                    LOG.error("Profiling failed for request {}: {}", finalRequestId, t.getMessage());
                    return List.of(errorMeta(
                            finalRequestId, ProfilerError.Type.PROFILE_EXCEPTION, t.getMessage()));
                })
                .onItem().transformToMulti(responses -> Multi.createFrom().iterable(responses));
    }

    private List<ProfileDataStreamResponse> runProfiling(
            String requestId, List<Map<String, Object>> records, List<String> rawJsonStrings) throws Exception {

        String schemaJson = schemaService.infer(records);
        ProfilingResult profilingResult = profilerService.profile(records, rawJsonStrings);
        String profileJson = objectMapper.writeValueAsString(profilingResult);
        byte[] profileBytes = profileJson.getBytes(StandardCharsets.UTF_8);

        List<ProfileDataStreamResponse> responses = new ArrayList<>();

        Meta meta = Meta.newBuilder()
                .setRequestId(requestId)
                .setSchema(schemaJson)
                .setTotalRecords(records.size())
                .setSchemaByteSize(schemaJson.getBytes(StandardCharsets.UTF_8).length)
                .setProfileByteSize(profileBytes.length)
                .setServiceVersion(config.serviceVersion())
                .build();

        responses.add(ProfileDataStreamResponse.newBuilder().setMeta(meta).build());

        // Chunk the JSON in case the profile is very large (unlikely but safe)
        for (String chunk : chunkUtf8(profileJson, CHUNK_SIZE)) {
            responses.add(ProfileDataStreamResponse.newBuilder().setProfile(chunk).build());
        }

        LOG.info("Profiled {} records for request '{}', JSON size: {} bytes, chunks: {}",
                records.size(), requestId, profileBytes.length, responses.size() - 1);

        return responses;
    }

    /**
     * Splits content into chunks whose UTF-8 encoded byte length does not exceed
     * maxBytesPerChunk, without ever splitting a single Unicode codepoint across
     * chunks (a raw byte-offset split can land inside a multi-byte UTF-8 sequence).
     */
    static List<String> chunkUtf8(String content, int maxBytesPerChunk) {
        List<String> chunks = new ArrayList<>();
        int length = content.length();
        int chunkStart = 0;
        int byteLenSoFar = 0;

        int i = 0;
        while (i < length) {
            int codepoint = content.codePointAt(i);
            int charCount = Character.charCount(codepoint);
            int byteLen = utf8ByteLength(codepoint);

            if (byteLenSoFar + byteLen > maxBytesPerChunk && i > chunkStart) {
                chunks.add(content.substring(chunkStart, i));
                chunkStart = i;
                byteLenSoFar = 0;
            }

            byteLenSoFar += byteLen;
            i += charCount;
        }
        if (chunkStart < length) {
            chunks.add(content.substring(chunkStart, length));
        }
        return chunks;
    }

    private static int utf8ByteLength(int codepoint) {
        if (codepoint <= 0x7F) return 1;
        if (codepoint <= 0x7FF) return 2;
        if (codepoint <= 0xFFFF) return 3;
        return 4;
    }

    private ProfileDataStreamResponse errorMeta(
            String requestId, ProfilerError.Type type, String message) {
        Meta meta = Meta.newBuilder()
                .setRequestId(requestId)
                .setServiceVersion(config.serviceVersion())
                .setError(ProfilerError.newBuilder()
                        .setType(type)
                        .setMessage(message != null ? message : "")
                        .build())
                .build();
        return ProfileDataStreamResponse.newBuilder().setMeta(meta).build();
    }
}
