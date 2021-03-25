package io.spoud.agoora.agents.mqtt.service;

import com.google.protobuf.StringValue;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.agoora.agents.api.client.MetricsClient;
import io.spoud.agoora.agents.api.mapper.StandardProtoMapper;
import io.spoud.agoora.agents.mqtt.Constants;
import io.spoud.agoora.agents.mqtt.config.data.MqttAgooraConfig;
import io.spoud.agoora.agents.mqtt.data.TopicDescription;
import io.spoud.agoora.agents.mqtt.repository.DataPortRepository;
import io.spoud.sdm.global.selection.v1.IdPathRef;
import io.spoud.sdm.logistics.domain.v1.DataPort;
import io.spoud.sdm.logistics.mutation.v1.PropertyMap;
import io.spoud.sdm.logistics.mutation.v1.StateChange;
import io.spoud.sdm.logistics.selection.v1.DataPortRef;
import io.spoud.sdm.logistics.selection.v1.ResourceGroupRef;
import io.spoud.sdm.logistics.selection.v1.TransportMatchingProperties;
import io.spoud.sdm.logistics.service.v1.DataPortChange;
import io.spoud.sdm.logistics.service.v1.SaveDataPortRequest;
import io.spoud.sdm.looker.v1alpha1.ResourceMetric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class DataService {

  private final DataPortRepository dataPortRepository;
  private final ReferenceService referenceService;
  private final DataPortClient dataPortClient;
  private final MetricsClient metricsClient;

  private final MqttAgooraConfig config;

  public void updateStates(TopicDescription topicDescription) {
    try {
      LOG.debug("Updating topic {}", topicDescription);

      final DataPort dataPort = uploadDataPort(topicDescription);
      dataPortRepository.update(dataPort);
    } catch (Exception ex) {
      LOG.error("Error while updating topic {}", topicDescription, ex);
    }
  }

  private DataPort uploadDataPort(final TopicDescription topicDescription) {
    StringValue shortName = StandardProtoMapper.stringValue(topicDescription.getDataPortTopic());

    Map<String, String> matchingProperties =
        Map.of(Constants.AGOORA_MATCHING_TOPIC_NAME, topicDescription.getDataPortTopic());
    Map<String, String> allProperties = new HashMap<>(matchingProperties);

    return dataPortClient.save(
        SaveDataPortRequest.newBuilder()
            .setInput(
                DataPortChange.newBuilder()
                    .setSelf(
                        DataPortRef.newBuilder()
                            .setTransportMatchingProperties(
                                TransportMatchingProperties.newBuilder()
                                    .setTransport(referenceService.getTransportRef())
                                    .putAllProperties(matchingProperties)
                                    .build())
                            .build())
                    .setState(StateChange.AVAILABLE)
                    .setLabel(shortName)
                    .setTransportUrl(shortName)
                    .setResourceGroup(
                        ResourceGroupRef.newBuilder()
                            .setIdPath(
                                IdPathRef.newBuilder()
                                    .setPath(
                                        config.getTransport().getAgooraPathObject().getResourceGroupPath())
                                    .buildPartial())
                            .build())
                    .setProperties(PropertyMap.newBuilder().putAllProperties(allProperties).build())
                    .setTransport(referenceService.getTransportRef())
                    .build())
            .build());
  }

  private void uploadMetric(String resourceId, ResourceMetric.MetricType type, double value) {
    try {
      metricsClient.updateMetric(resourceId, type, value);
    } catch (Exception ex) {
      LOG.error("Unable to send metric for resourceId {} and type {}", resourceId, type, ex);
    }
  }

  public void updateMetrics(
      TopicDescription topicDescription, double msg_per_duration, double bytes_per_duration) {

    dataPortRepository
        .getDataPortByTopicDescription(topicDescription)
        .ifPresentOrElse(
            dataPort -> {
              uploadMetric(
                  dataPort.getId(), ResourceMetric.MetricType.DATA_PORT_MESSAGES, msg_per_duration);
              uploadMetric(
                  dataPort.getId(), ResourceMetric.MetricType.DATA_PORT_BYTES, bytes_per_duration);
              LOG.info(
                  "Metric for topic {} : msg/duration={} bytes/duration={}",
                  topicDescription.getDataPortTopic(),
                  msg_per_duration,
                  bytes_per_duration);
            },
            () -> LOG.error("No data port found for topic {}", topicDescription));
  }
}
