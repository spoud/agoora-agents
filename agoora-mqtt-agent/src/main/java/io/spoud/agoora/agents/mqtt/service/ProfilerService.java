package io.spoud.agoora.agents.mqtt.service;

import com.google.protobuf.Timestamp;
import io.spoud.agoora.agents.api.client.BlobClient;
import io.spoud.agoora.agents.api.client.LookerClient;
import io.spoud.agoora.agents.api.client.ProfilerClient;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.api.mapper.StandardProtoMapper;
import io.spoud.agoora.agents.api.observers.ProfileResponseObserver;
import io.spoud.agoora.agents.mqtt.config.data.MqttAgooraConfig;
import io.spoud.agoora.agents.mqtt.data.TopicDescription;
import io.spoud.agoora.agents.mqtt.repository.DataPortRepository;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.global.selection.v1.EntityRef;
import io.spoud.sdm.logistics.domain.v1.DataPort;
import io.spoud.sdm.looker.domain.v1alpha1.DataProfilingError;
import io.spoud.sdm.looker.v1alpha1.AddDataProfileRequest;
import io.spoud.sdm.profiler.domain.v1alpha1.ProfilerError;
import io.spoud.sdm.schema.domain.v1alpha.SchemaEncoding;
import io.spoud.sdm.schema.domain.v1alpha.SchemaSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ProfilerService {

  private final BlobClient blobClient;
  private final ProfilerClient profilerClient;
  private final LookerClient lookerClient;
  private final SchemaClient schemaClient;

  private final DataPortRepository dataPortRepository;

  private final MqttAgooraConfig config;

  // profiling only supports one at the time
  private final ExecutorService managedExecutor = Executors.newSingleThreadExecutor();

  public static final Timestamp now() {
    Instant now = Instant.now();
    return StandardProtoMapper.timestamp(now);
  }

  public void profileMqttMessages(TopicDescription topicDescription, List<MqttMessage> messages) {
    dataPortRepository
        .getDataPortByTopicDescription(topicDescription)
        .ifPresentOrElse(
            dataPort ->
                this.managedExecutor.execute(
                    () -> this.profileMqttMessages(dataPort, topicDescription, messages)),
            () -> LOG.error("No data port found for topic {}", topicDescription));
  }

  private void profileMqttMessages(
      DataPort dataport, TopicDescription topicDescription, List<MqttMessage> messages) {
    Instant start = Instant.now();
    String topicName = topicDescription.getDataPortTopic();
    String requestId = dataport.getEndpointUrl() + "?profileJob=" + dataport.getId();
    AtomicInteger sampleSize = new AtomicInteger(0);
    LOG.debug("Start profile dataPort {} for topicName {}", dataport.getId(), topicName);

    final List<byte[]> sampleBytes =
        messages.stream().map(MqttMessage::getPayload).collect(Collectors.toList());

    sampleSize.set(sampleBytes.size());
    try {
      AddDataProfileRequest.Builder dataProfileRequest =
          AddDataProfileRequest.newBuilder()
              .setDataSamplesCount(sampleBytes.size())
              .setReportTimestamp(now())
              .setEntityRef(
                  EntityRef.newBuilder()
                      .setEntityType(ResourceEntity.Type.DATA_PORT)
                      .setId(dataport.getId())
                      .build());

      if (sampleBytes.isEmpty()) {
        LOG.warn("No data for table {}", topicName);
        dataProfileRequest.setError(
            DataProfilingError.newBuilder()
                .setType(DataProfilingError.Type.NO_DATA)
                .buildPartial());
      } else {
        LOG.debug("Profiling some samples of table {}: {}", topicName, sampleBytes.size());

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
                      DataProfilingError.Type.UNKNOWN_ENCODING) // TODO map profilerError.type ?
                  .buildPartial());
        } else {
          // upload schema
          uploadSchema(topicDescription, dataport, profilerResponse);

          // take care of profiler result
          String html = profilerResponse.getHtml();

          LOG.debug("Profile received for table {}: {}bytes", topicName, html.length());
          String htmlId =
              blobClient.uploadBlobUtf8(
                  html,
                  config.getTransport().getAgooraPathObject().getResourceGroupPath(),
                  ResourceEntity.Type.DATA_PORT);
          if (htmlId != null) {
            dataProfileRequest.setProfileHtmlBlobId(htmlId);
          }
        }
      }
      lookerClient.addDataProfile(dataProfileRequest.build());
    } catch (Exception ex) {
      LOG.error("Unable to send samples for table {}", topicName, ex);
    }
    LOG.info(
        "Processing of data port {} for topic {} with {} samples took {}",
        dataport.getId(),
        topicName,
        sampleSize.get(),
        Duration.between(start, Instant.now()));
  }

  private void uploadSchema(
      TopicDescription topicDescription,
      DataPort dataPort,
      ProfileResponseObserver.ProfilerResponse profileResponse) {
    String schemaContent = null;
    if (profileResponse.getSchema() != null) {
      schemaContent = profileResponse.getSchema();
    } else if (profileResponse.getMeta() != null && profileResponse.getMeta().getSchema() != null) {
      schemaContent = profileResponse.getMeta().getSchema();
    }
    if (schemaContent != null) {
      try {
        String schemaId =
            schemaClient
                .saveSchema(
                    ResourceEntity.Type.DATA_PORT,
                    dataPort.getId(),
                    config.getTransport().getAgooraPathObject().getResourceGroupPath(),
                    schemaContent,
                    SchemaSource.Type.INFERRED,
                    SchemaEncoding.Type.JSON)
                .getId();
        LOG.info("Schema {} saved for data port {}", schemaId, dataPort.getId());
      } catch (Exception ex) {
        LOG.error("Unable to send schema for data port {}", dataPort.getId(), ex);
      }
    } else {
      LOG.warn(
          "No schema content for data port {} and topic {}",
          dataPort.getId(),
          topicDescription.getDataPortTopic());
    }
  }
}
