package io.spoud.agoora.agents.pgsql.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import io.spoud.agoora.agents.api.client.LookerClient;
import io.spoud.agoora.agents.api.client.ProfilerClient;
import io.spoud.agoora.agents.api.model.DataProfileEnvelope;
import io.spoud.agoora.agents.api.observers.ProfileResponseObserver;
import io.spoud.agoora.agents.pgsql.config.data.PgsqlAgooraConfig;
import io.spoud.agoora.agents.pgsql.database.DatabaseScrapper;
import io.spoud.agoora.agents.pgsql.repository.DataItemRepository;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.global.selection.v1.EntityRef;
import io.spoud.sdm.logistics.domain.v1.DataItem;
import io.spoud.sdm.looker.domain.v1alpha1.DataProfilingError;
import io.spoud.sdm.looker.v1alpha1.AddDataProfileRequest;
import io.spoud.sdm.profiler.domain.v1alpha1.ProfilerError;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static io.spoud.agoora.agents.api.mapper.StandardProtoMapper.timestamp;


@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ProfilerService {

  private final DatabaseScrapper databaseScrapper;
  private final DataItemRepository dataItemRepository;

  private final ProfilerClient profilerClient;
  private final LookerClient lookerClient;

  private final ObjectMapper objectMapper;
  private final PgsqlAgooraConfig config;

  public static final Timestamp now() {
    Instant now = Instant.now();
    return timestamp(now);
  }

  public void runProfiler() {
    dataItemRepository.findAll().forEach(this::profileTable);
  }

  private void profileTable(DataItem dataItem) {
    Instant start = Instant.now();
    String tableName = dataItem.getTransportUrl();
    String requestId = dataItem.getTransportUrl() + "?profileJob=" + dataItem.getId();
    AtomicInteger sampleSize = new AtomicInteger(0);
    LOG.debug("Start profile dataItem {} for table {}", dataItem.getId(), tableName);

    databaseScrapper
        .getSamples(tableName, config.scrapper().samplesSize())
        .ifPresent(
            samples -> {
              sampleSize.set(samples.size());
              try {
                AddDataProfileRequest.Builder dataProfileRequest =
                    AddDataProfileRequest.newBuilder()
                        .setDataSamplesCount(samples.size())
                        .setReportTimestamp(now())
                        .setEntityRef(
                            EntityRef.newBuilder()
                                .setEntityType(ResourceEntity.Type.DATA_ITEM)
                                .setId(dataItem.getId())
                                .build());

                if (samples.isEmpty()) {
                  LOG.warn("No data for table {}", tableName);
                  dataProfileRequest.setError(
                      DataProfilingError.newBuilder()
                          .setType(DataProfilingError.Type.NO_DATA)
                          .buildPartial());
                } else {
                  LOG.debug("Profiling some samples of table {}: {}", tableName, samples.size());

                  final List<byte[]> sampleBytes =
                      samples.stream().map(this::getJson).collect(Collectors.toList());

                  final ProfileResponseObserver.ProfilerResponse profilerResponse =
                      profilerClient.profileData(requestId, sampleBytes);

                  Optional<ProfilerError> error = profilerResponse.getError();

                  if (error.isPresent()) {
                    ProfilerError profilerError = error.get();
                    LOG.error("Error while profiling {}", profilerError.getMessage());
                    dataProfileRequest.setError(
                        DataProfilingError.newBuilder()
                            .setMessage(profilerError.getMessage())
                            .setType(
                                DataProfilingError.Type
                                    .UNKNOWN_ENCODING) // TODO map profilerError.type ?
                            .buildPartial());
                  } else {
                    if (profilerResponse.hasProfileJson()) {
                      String profileJson = profilerResponse.getProfileJson();
                      LOG.debug("Profile received for table {}: {}bytes", tableName, profileJson.length());
                      DataProfileEnvelope envelope = DataProfileEnvelope.wrap(profileJson);
                      dataProfileRequest.setProfileJson(objectMapper.writeValueAsString(envelope));
                    } else {
                      LOG.warn("Profile JSON content is null or blank for table {}", tableName);
                    }
                  }
                }
                lookerClient.addDataProfile(dataProfileRequest.build());
              } catch (Exception ex) {
                LOG.error("Unable to send samples for table {}", tableName, ex);
              }
            });
    LOG.info(
        "Processing of data item {} for table {} with {} samples took {}",
        dataItem.getId(),
        tableName,
        sampleSize.get(),
        Duration.between(start, Instant.now()));
  }

  @SneakyThrows
  private byte[] getJson(Map<String, Object> obj) {
    return objectMapper.writeValueAsBytes(obj);
  }
}
