package io.spoud.agoora.agents.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.spoud.agoora.agents.api.client.LookerClient;
import io.spoud.agoora.agents.api.client.ProfilerClient;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.api.mapper.StandardProtoMapper;
import io.spoud.agoora.agents.api.model.DataProfileEnvelope;
import io.spoud.agoora.agents.api.observers.ProfileResponseObserver;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.agoora.agents.kafka.data.*;
import io.spoud.agoora.agents.kafka.decoder.DecodedMessages;
import io.spoud.agoora.agents.kafka.decoder.DecoderService;
import io.spoud.agoora.agents.kafka.kafka.KafkaTopicReader;
import io.spoud.agoora.agents.kafka.repository.KafkaTopicRepository;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.global.selection.v1.EntityRef;
import io.spoud.sdm.looker.domain.v1alpha1.DataProfilingError;
import io.spoud.sdm.looker.v1alpha1.AddDataProfileRequest;
import io.spoud.sdm.profiler.domain.v1alpha1.ProfilerError;
import io.spoud.sdm.schema.domain.v1alpha.SchemaEncoding;
import io.spoud.sdm.schema.domain.v1alpha.SchemaSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ProfilerService {

  private static final int KEY_SAMPLE_LIMIT = 5;

  private final KafkaTopicRepository kafkaTopicRepository;
  private final KafkaTopicReader kafkaTopicReader;
  private final ProfilerClient profilerClient;
  private final LookerClient lookerClient;
  private final SchemaClient schemaClient;
  private final KafkaAgentConfig config;
  private final DecoderService decoderService;
  private final PartitionAnalysisService partitionAnalysisService;
  private final EncodingDetectionService encodingDetectionService;
  private final ObjectMapper objectMapper;

  public void profileData() {
    kafkaTopicRepository.getStates().stream()
        .filter(topic -> topic.getDataPortId() != null)
        .forEach(
            kafkaTopic -> {
              try {
                KafkaSampleResult sampleResult = kafkaTopicReader.getSamples(kafkaTopic.getTopicName());

                List<byte[]> rawValueBytes = sampleResult.getValueBytes();

                EncodingDetectionService.EncodingResult encodingResult =
                    encodingDetectionService.detectEncoding(rawValueBytes);

                DecodedMessages decodedValues = decoderService.decodeValue(kafkaTopic.getTopicName(), rawValueBytes);
                List<byte[]> decodedSamples = decodedValues.getMessages();
                String valueFormat = decodedValues.getEncoding().name();

                PartitionAnalysis partitionAnalysis = partitionAnalysisService.analyze(sampleResult);

                KeyAnalysisResult keyAnalysis = analyzeKeys(kafkaTopic, sampleResult);

                profileSamples(kafkaTopic, decodedSamples, valueFormat, encodingResult, partitionAnalysis, keyAnalysis);
              } catch (Exception ex) {
                if (LOG.isDebugEnabled()) {
                  LOG.warn("Unable to profile topic '{}'", kafkaTopic.getTopicName(), ex);
                } else {
                  LOG.warn(
                      "Unable to profile topic '{}', skipping. Enable debug for full stacktrace: {}",
                      kafkaTopic.getTopicName(),
                      ex.getMessage());
                }

                lookerClient.addDataProfile(
                    AddDataProfileRequest.newBuilder()
                        .setEntityRef(getEntityRef(kafkaTopic))
                        .setReportTimestamp(StandardProtoMapper.timestamp(Instant.now()))
                        .setError(
                            DataProfilingError.newBuilder()
                                .setType(DataProfilingError.Type.UNKNOWN_ENCODING)
                                .setMessage(ex.getMessage())
                                .build())
                        .build());
              }
            });
  }


  private KeyAnalysisResult analyzeKeys(KafkaTopic kafkaTopic, KafkaSampleResult sampleResult) {
    List<byte[]> keyBytes = sampleResult.getKeyBytes();
    int totalRecords = sampleResult.getRecords().size();
    int keysPresent = keyBytes.size();

    if (keysPresent == 0) {
      return KeyAnalysisResult.builder()
              .keyFormat("NONE")
              .presenceRate(0.0)
              .totalRecords(totalRecords)
              .keysPresent(0)
              .build();
    }

    double presenceRate = (double) keysPresent / totalRecords;

    LongSummaryStatistics sizeStats = keyBytes.stream()
            .mapToLong(b -> b.length)
            .summaryStatistics();

    Set<String> uniqueKeys = keyBytes.stream()
            .map(b -> new String(b, StandardCharsets.UTF_8))
            .collect(Collectors.toSet());

    List<String> sampleValues = keyBytes.stream()
            .limit(KEY_SAMPLE_LIMIT)
            .map(b -> new String(b, StandardCharsets.UTF_8))
            .toList();

    String fullProfileJson = null;
    String keyFormat;

    try {
      DecodedMessages decodedKeys = decoderService.decodeKey(kafkaTopic.getTopicName(), keyBytes);
      keyFormat = decodedKeys.getEncoding().name();

      if (!decodedKeys.getMessages().isEmpty()) {
        ProfileResponseObserver.ProfilerResponse keyProfileResponse =
                profilerClient.profileData(kafkaTopic.getTopicName() + "-key", decodedKeys.getMessages());
        if (keyProfileResponse.getError().isEmpty() && keyProfileResponse.hasProfileJson()) {
          fullProfileJson = keyProfileResponse.getProfileJson();
        }
      }
    } catch (Exception e) {
      LOG.debug("Key decoding failed for topic {}: {}", kafkaTopic.getTopicName(), e.getMessage());
      keyFormat = "STRING";
      try {
        List<byte[]> wrappedKeys = keyBytes.stream()
                .map(b -> new String(b, StandardCharsets.UTF_8))
                .filter(s -> !s.isBlank())
                .map(s -> {
                  try {
                    return objectMapper.writeValueAsBytes(Map.of("key", s));
                  } catch (Exception ex) {
                    return null;
                  }
                })
                .filter(Objects::nonNull)
                .toList();

        if (!wrappedKeys.isEmpty()) {
          ProfileResponseObserver.ProfilerResponse keyProfileResponse =
                  profilerClient.profileData(kafkaTopic.getTopicName() + "-key", wrappedKeys);
          if (keyProfileResponse.getError().isEmpty() && keyProfileResponse.hasProfileJson()) {
            fullProfileJson = keyProfileResponse.getProfileJson();
          }
        }
      } catch (Exception ex) {
        LOG.debug("String key profiling failed for topic {}: {}", kafkaTopic.getTopicName(), ex.getMessage());
        keyFormat = "BINARY";
      }
    }

    return KeyAnalysisResult.builder()
            .keyFormat(keyFormat)
            .presenceRate(Math.round(presenceRate * 10000.0) / 10000.0)
            .totalRecords(totalRecords)
            .keysPresent(keysPresent)
            .uniqueCount(uniqueKeys.size())
            .minByteSize(sizeStats.getMin())
            .maxByteSize(sizeStats.getMax())
            .avgByteSize(Math.round(sizeStats.getAverage() * 100.0) / 100.0)
            .sampleValues(sampleValues)
            .fullProfileJson(fullProfileJson)
            .build();
  }

  private void profileSamples(
          KafkaTopic kafkaTopic,
          List<byte[]> samples,
          String valueFormat,
          EncodingDetectionService.EncodingResult encodingResult,
          PartitionAnalysis partitionAnalysis,
          KeyAnalysisResult keyAnalysis) {
    Instant start = Instant.now();
    String requestId = kafkaTopic.getTopicName();
    LOG.debug("Start profile topic {} with {} samples", kafkaTopic, samples.size());

    try {
      AddDataProfileRequest.Builder dataProfileRequest =
          AddDataProfileRequest.newBuilder()
              .setDataSamplesCount(samples.size())
              .setReportTimestamp(StandardProtoMapper.timestamp(start))
              .setEntityRef(getEntityRef(kafkaTopic));

      if (samples.isEmpty()) {
        LOG.warn("No data for topic {}", kafkaTopic);
        dataProfileRequest.setError(
            DataProfilingError.newBuilder()
                .setType(DataProfilingError.Type.NO_DATA)
                .buildPartial());
      } else {
        LOG.debug("Profiling some samples of topic {}: {}", kafkaTopic, samples.size());

        final ProfileResponseObserver.ProfilerResponse profilerResponse =
            profilerClient.profileData(requestId, samples);

        Optional<ProfilerError> error = profilerResponse.getError();

        if (error.isPresent()) {
          ProfilerError profilerError = error.get();
          LOG.error("Error while profiling {}", profilerError.getMessage());
          dataProfileRequest.setError(
              DataProfilingError.newBuilder()
                  .setMessage(profilerError.getMessage())
                  .setType(DataProfilingError.Type.UNKNOWN_ENCODING)
                  .buildPartial());
        } else {
          uploadSchema(kafkaTopic, profilerResponse);

          if (profilerResponse.hasProfileJson()) {
            String valueProfileJson = profilerResponse.getProfileJson();
            LOG.debug("Profile received for topic {}: {}bytes", kafkaTopic, valueProfileJson.length());

            Map<String, Object> sourceMetadata = new LinkedHashMap<>();
            sourceMetadata.put("valueFormat", valueFormat);
            sourceMetadata.put("partitionAnalysis", partitionAnalysis);
            sourceMetadata.put("keyAnalysis", keyAnalysis);

            DataProfileEnvelope envelope = DataProfileEnvelope.builder()
                    .version("3")
                    .valueProfile(valueProfileJson)
                    .keyProfile(keyAnalysis.getFullProfileJson())
                    .valueEncoding(DataProfileEnvelope.ValueEncoding.builder()
                            .charset(encodingResult.getCharset())
                            .confidence(encodingResult.getConfidence())
                            .build())
                    .sourceMetadata(sourceMetadata)
                    .build();

            String enrichedJson = objectMapper.writeValueAsString(envelope);
            dataProfileRequest.setProfileJson(enrichedJson);
          } else {
            LOG.warn("Profile JSON content is null or blank for topic {}", kafkaTopic);
          }
        }
      }
      lookerClient.addDataProfile(dataProfileRequest.build());
    } catch (Exception ex) {
      LOG.error("Unable to send samples for table {}", kafkaTopic, ex);
    }
    LOG.info(
        "Processing of data port {} for topic {} with {} samples took {}",
        kafkaTopic.getDataPortId(),
        kafkaTopic.getTopicName(),
        samples.size(),
        Duration.between(start, Instant.now()));
  }

  private EntityRef getEntityRef(KafkaTopic kafkaTopic) {
    return EntityRef.newBuilder()
        .setEntityType(ResourceEntity.Type.DATA_PORT)
        .setId(kafkaTopic.getDataPortId())
        .build();
  }

  protected void uploadSchema(
      KafkaTopic kafkaTopic, ProfileResponseObserver.ProfilerResponse profileResponse) {
    String schemaContent = profileResponse.getSchema();
    if (schemaContent != null) {
      try {
        String schemaId =
            schemaClient
                .saveSchema(
                    ResourceEntity.Type.DATA_PORT,
                    kafkaTopic.getDataPortId(),
                    config.transport().getAgooraPathObject().getResourceGroupPath(),
                    schemaContent,
                    SchemaSource.Type.INFERRED,
                    SchemaEncoding.Type.JSON,
                    "",
                    SchemaEncoding.Type.UNKNOWN
                )
                .getId();
        LOG.info("Profiler Schema {} saved for data port {}", schemaId, kafkaTopic.getDataPortId());
      } catch (Exception ex) {
        LOG.error("Unable to send profiler schema for data port {}", kafkaTopic.getDataPortId(), ex);
      }
    } else {
      LOG.warn(
          "No schema content for data port {} and topic {}",
          kafkaTopic.getDataPortId(),
          kafkaTopic.getTopicName());
    }
  }
}
