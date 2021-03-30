package io.spoud.agoora.agents.kafka.logistics;

import com.google.protobuf.StringValue;
import io.grpc.StatusRuntimeException;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.agoora.agents.api.client.DataSubscriptionStateClient;
import io.spoud.agoora.agents.kafka.Constants;
import io.spoud.agoora.agents.kafka.data.KafkaConsumerGroup;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import io.spoud.sdm.global.selection.v1.BaseRef;
import io.spoud.sdm.global.selection.v1.IdPathRef;
import io.spoud.sdm.logistics.domain.v1.DataPort;
import io.spoud.sdm.logistics.domain.v1.DataSubscriptionState;
import io.spoud.sdm.logistics.mutation.v1.PropertyMap;
import io.spoud.sdm.logistics.mutation.v1.StateChange;
import io.spoud.sdm.logistics.selection.v1.DataPortRef;
import io.spoud.sdm.logistics.selection.v1.DataSubscriptionStateRef;
import io.spoud.sdm.logistics.selection.v1.ResourceGroupRef;
import io.spoud.sdm.logistics.selection.v1.TransportMatchingProperties;
import io.spoud.sdm.logistics.service.v1.DataPortChange;
import io.spoud.sdm.logistics.service.v1.DataSubscriptionStateChange;
import io.spoud.sdm.logistics.service.v1.SaveDataPortRequest;
import io.spoud.sdm.logistics.service.v1.SaveDataSubscriptionStateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.util.Optional;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class LogisticsService {

  private final DataPortClient dataPortClient;
  private final DataSubscriptionStateClient dataSubscriptionStateClient;
  private final LogisticsRefService logisticsRefService;

  public Optional<DataPort> updateDataPort(final KafkaTopic dataPort) {
    String topicName = dataPort.getTopicName();
    LOG.debug("Updating data offer state with topic name {}", topicName);
    final DataPort saved;
    try {
      saved =
          dataPortClient.save(
              SaveDataPortRequest.newBuilder()
                  .setInput(
                      DataPortChange.newBuilder()
                          .setSelf(
                              DataPortRef.newBuilder()
                                  .setTransportMatchingProperties(
                                      TransportMatchingProperties.newBuilder()
                                          .setTransport(
                                              BaseRef.newBuilder()
                                                  .setIdPath(logisticsRefService.getTransportRef())
                                                  .build())
                                          .putProperties(
                                              Constants.SDM_PROPERTIES_KAFKA_TOPIC,
                                              dataPort.getTopicName())
                                          .build())
                                  .build())
                          .setLabel(
                              StringValue.newBuilder().setValue(dataPort.getTopicName()).build())
                          .setTransportUrl(
                              StringValue.newBuilder().setValue(dataPort.getTransportUrl()))
                          .setResourceGroup(
                              ResourceGroupRef.newBuilder()
                                  .setIdPath(logisticsRefService.getResourceGroupRef())
                                  .build())
                          .setProperties(
                              PropertyMap.newBuilder().putAllProperties(dataPort.getProperties()))
                          .setState(StateChange.AVAILABLE)
                          .build())
                  .build());
    } catch (final StatusRuntimeException e) {
      LOG.error("Error while updating data offer state in logistics, will skip and continue.", e);
      return Optional.empty();
    }
    LOG.info(
        "Updated data offer state with id '{}' and name '{}' for topic '{}'",
        saved.getId(),
        saved.getName(),
        topicName);
    return Optional.of(saved);
  }

  public Optional<DataSubscriptionState> updateDataSubscriptionState(
      final KafkaConsumerGroup dataSubscriptionState) {
    String topicName = dataSubscriptionState.getTopicName();
    String consumerGroupName = dataSubscriptionState.getConsumerGroupName();
    LOG.debug(
        "Updating data subscription state for consumer group {} and topic name {}.",
        consumerGroupName,
        topicName);
    final DataSubscriptionState saved;
    try {
      saved =
          dataSubscriptionStateClient.save(
              SaveDataSubscriptionStateRequest.newBuilder()
                  .setInput(
                      DataSubscriptionStateChange.newBuilder()
                          .setSelf(
                              DataSubscriptionStateRef.newBuilder()
                                  .setTransportMatchingProperties(
                                      TransportMatchingProperties.newBuilder()
                                          .setTransport(
                                              BaseRef.newBuilder()
                                                  .setIdPath(logisticsRefService.getTransportRef())
                                                  .build())
                                          .putProperties(
                                              Constants.SDM_PROPERTIES_KAFKA_TOPIC,
                                              dataSubscriptionState.getTopicName())
                                          .putProperties(
                                              Constants.SDM_PROPERTIES_KAFKA_CONSUMER_GROUP,
                                              dataSubscriptionState.getConsumerGroupName())
                                          .build())
                                  .build())
                          .setLabel(
                              StringValue.newBuilder()
                                  .setValue(dataSubscriptionState.getConsumerGroupName())
                                  .build())
                          .setTransportUrl(
                              StringValue.newBuilder()
                                  .setValue(dataSubscriptionState.getTransportUrl()))
                          .setResourceGroup(
                              ResourceGroupRef.newBuilder()
                                  .setIdPath(logisticsRefService.getResourceGroupRef())
                                  .build())
                          .setDataPort(
                              DataPortRef.newBuilder()
                                  .setTransportMatchingProperties(
                                      TransportMatchingProperties.newBuilder()
                                          .setTransport(
                                              BaseRef.newBuilder()
                                                  .setIdPath(logisticsRefService.getTransportRef())
                                                  .build())
                                          .putProperties(
                                              Constants.SDM_PROPERTIES_KAFKA_TOPIC,
                                              dataSubscriptionState.getTopicName()))
                                  .build())
                          .setProperties(
                              PropertyMap.newBuilder()
                                  .putAllProperties(dataSubscriptionState.getProperties()))
                          .setState(StateChange.AVAILABLE)
                          .build())
                  .build());
    } catch (final StatusRuntimeException e) {
      LOG.error(
          "Error while updating data subscription state in logistics for consumer group '{}' and topic '{}', will skip and continue. {}",
          consumerGroupName,
          topicName,
          e);
      return Optional.empty();
    }
    LOG.info(
        "Updated data subscription state with id '{}' and name '{}' for consumer group '{}' and topic '{}'",
        saved.getId(),
        saved.getName(),
        consumerGroupName,
        topicName);
    return Optional.of(saved);
  }

  public Optional<DataPort> deleteDataPort(final KafkaTopic dataPort) {
    LOG.debug("Inactivating data offer state name {}", dataPort.getTopicName());
    if (dataPort.getDataPortId() == null) {
      throw new IllegalStateException("Got a KafkaTopic without a dataPortId.");
    }
    final DataPort saved;
    try {
      saved =
          dataPortClient.save(
              SaveDataPortRequest.newBuilder()
                  .setInput(
                      DataPortChange.newBuilder()
                          .setSelf(
                              DataPortRef.newBuilder()
                                  .setIdPath(
                                      IdPathRef.newBuilder()
                                          .setId(dataPort.getDataPortId())
                                          .build())
                                  .build())
                          .setState(StateChange.DELETED)
                          .build())
                  .build());
    } catch (final StatusRuntimeException e) {
      LOG.error(
          "Error while updating data offer state in logistics (set state to deleted), will skip and continue.",
          e);
      return Optional.empty();
    }
    LOG.info(
        "Inactivated data offer state with id '{}' and name '{}'", saved.getId(), saved.getName());
    return Optional.of(saved);
  }

  public Optional<DataSubscriptionState> deleteDataSubscriptionState(
      final KafkaConsumerGroup dataSubscriptionState) {
    LOG.debug(
        "Inactivating data subscription state name {}",
        dataSubscriptionState.getConsumerGroupName());
    final DataSubscriptionState saved;
    try {
      saved =
          dataSubscriptionStateClient.save(
              SaveDataSubscriptionStateRequest.newBuilder()
                  .setInput(
                      DataSubscriptionStateChange.newBuilder()
                          .setSelf(
                              DataSubscriptionStateRef.newBuilder()
                                  .setIdPath(
                                      IdPathRef.newBuilder()
                                          .setId(dataSubscriptionState.getDataSubscriptionStateId())
                                          .build())
                                  .build())
                          .setState(StateChange.DELETED)
                          .build())
                  .build());
    } catch (final StatusRuntimeException e) {
      LOG.error(
          "Error while updating data subscription state in logistics, will skip and continue.", e);
      return Optional.empty();
    }
    LOG.info(
        "Inactivated data subscription state with id '{}' and name '{}'",
        saved.getId(),
        saved.getName());
    return Optional.of(saved);
  }
}
