package io.spoud.agoora.agents.kafka.service;

import io.spoud.agoora.agents.api.client.BlobClient;
import io.spoud.agoora.agents.api.client.LookerClient;
import io.spoud.agoora.agents.api.client.ProfilerClient;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.api.mapper.StandardProtoMapper;
import io.spoud.agoora.agents.api.observers.ProfileResponseObserver;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
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

import javax.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ProfilerService {

  private final KafkaTopicRepository kafkaTopicRepository;
  private final KafkaTopicReader kafkaTopicReader;
  private final BlobClient blobClient;
  private final ProfilerClient profilerClient;
  private final LookerClient lookerClient;
  private final SchemaClient schemaClient;
  private final KafkaAgentConfig config;
  private final DecoderService decoderService;

  public void profileData() {
    kafkaTopicRepository.getStates().stream()
        .filter(topic -> topic.getDataPortId() != null)
        .forEach(
            kafkaTopic -> {
              try {
                List<byte[]> samples = kafkaTopicReader.getSamples(kafkaTopic.getTopicName());

                samples = decodeMessages(kafkaTopic, samples);

                profileSamples(kafkaTopic, samples);
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

  private List<byte[]> decodeMessages(KafkaTopic kafkaTopic, List<byte[]> samples) {
    return decoderService.decodeValue(kafkaTopic.getTopicName(), samples).getMessages();
  }

  private void profileSamples(KafkaTopic kafkaTopic, List<byte[]> samples) {
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
                  .setType(
                      DataProfilingError.Type.UNKNOWN_ENCODING) // TODO map profilerError.type ?
                  .buildPartial());
        } else {
          // upload schema
          uploadSchema(kafkaTopic, profilerResponse);

          // take care of profiler result
          String html = profilerResponse.getHtml();

          if (html != null) {
            LOG.debug("Profile received for table {}: {}bytes", kafkaTopic, html.length());
            String htmlId =
                blobClient.uploadBlobUtf8(
                    html,
                    config.transport().getAgooraPathObject().getResourceGroupPath(),
                    ResourceEntity.Type.DATA_PORT);
            if (htmlId != null) {
              dataProfileRequest.setProfileHtmlBlobId(htmlId);
            }
          } else {
            LOG.warn("Html content is null");
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
                    "", // TODO: add schema key content from profile
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
