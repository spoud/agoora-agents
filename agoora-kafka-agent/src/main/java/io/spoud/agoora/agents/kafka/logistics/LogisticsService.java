package io.spoud.agoora.agents.kafka.logistics;

import com.google.protobuf.StringValue;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.agoora.agents.api.client.DataSubscriptionStateClient;
import io.spoud.agoora.agents.kafka.Constants;
import io.spoud.agoora.agents.kafka.data.KafkaConsumerGroup;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import io.spoud.agoora.agents.kafka.service.PropertyTemplateService;
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

import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class LogisticsService {

  private final DataPortClient dataPortClient;
  private final DataSubscriptionStateClient dataSubscriptionStateClient;
  private final LogisticsRefService logisticsRefService;
  private final PropertyTemplateService propertyTemplateService;

  /**
   * Ids whose removal logistics refused, so the refusal is only logged once per agent run while
   * the removal keeps being retried every iteration.
   */
  private final Set<String> refusedRemovals = ConcurrentHashMap.newKeySet();

  /**
   * All data ports of this agent's transport that logistics considers available. Empty when
   * logistics cannot be reached, so callers can skip deletions instead of acting on partial data.
   */
  public Optional<List<DataPort>> listAvailableDataPorts() {
    try {
      final String transportId = logisticsRefService.getTransportId();
      return Optional.of(
          ofTransport(
              dataPortClient.listAvailable(logisticsRefService.getTransportRef()),
              transportId,
              p -> p.getTransport().getId(),
              DataPort::getDeleted,
              "data ports"));
    } catch (final StatusRuntimeException | IllegalStateException e) {
      LOG.error("Error while listing data ports from logistics, will skip removals this time.", e);
      return Optional.empty();
    }
  }

  /** Same as {@link #listAvailableDataPorts()} for data subscription states. */
  public Optional<List<DataSubscriptionState>> listAvailableDataSubscriptionStates() {
    try {
      final String transportId = logisticsRefService.getTransportId();
      return Optional.of(
          ofTransport(
              dataSubscriptionStateClient.listAvailable(logisticsRefService.getTransportRef()),
              transportId,
              s -> s.getTransport().getId(),
              DataSubscriptionState::getDeleted,
              "data subscription states"));
    } catch (final StatusRuntimeException | IllegalStateException e) {
      LOG.error(
          "Error while listing data subscription states from logistics, will skip removals this time.",
          e);
      return Optional.empty();
    }
  }

  /**
   * Keep only the entities of this agent's transport that are not deleted. Logistics already
   * filters on both, this is a safeguard: the agent removes whatever is in the list and missing in
   * Kafka, so an entity of another transport slipping through must never be removed.
   */
  private static <T> List<T> ofTransport(
      List<T> entities,
      String transportId,
      Function<T, String> transportIdOf,
      Predicate<T> deleted,
      String what) {
    final List<T> result =
        entities.stream()
            .filter(e -> transportId.equals(transportIdOf.apply(e)) && !deleted.test(e))
            .toList();
    if (result.size() != entities.size()) {
      LOG.warn(
          "Logistics listed {} {} that are deleted or belong to another transport than {}, ignoring"
              + " them.",
          entities.size() - result.size(),
          what,
          transportId);
    }
    return result;
  }

  public Optional<DataPort> updateDataPort(final KafkaTopic dataPort) {
    String topicName = dataPort.getTopicName();
    LOG.debug("Updating data offer state with topic name {}", topicName);

    Map<String, String> properties = new HashMap<>();
    properties.putAll(dataPort.getProperties());
    properties.putAll(propertyTemplateService.mapExternalPropertiesForTopic(dataPort));

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
                                              Constants.AGOORA_PROPERTIES_KAFKA_TOPIC,
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
                              PropertyMap.newBuilder().putAllProperties(properties))
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

    Map<String, String> properties = new HashMap<>();
    properties.putAll(dataSubscriptionState.getProperties());
    properties.putAll(
        propertyTemplateService.mapExternalPropertiesForConsumerGroup(dataSubscriptionState));

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
                                              Constants.AGOORA_PROPERTIES_KAFKA_TOPIC,
                                              dataSubscriptionState.getTopicName())
                                          .putProperties(
                                              Constants.AGOORA_PROPERTIES_KAFKA_CONSUMER_GROUP,
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
                                              Constants.AGOORA_PROPERTIES_KAFKA_TOPIC,
                                              dataSubscriptionState.getTopicName()))
                                  .build())
                          .setProperties(PropertyMap.newBuilder().putAllProperties(properties))
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
      if (isRefused(e)) {
        if (refusedRemovals.add(dataPort.getDataPortId())) {
          LOG.warn(
              "Data port '{}' (topic '{}') is gone from Kafka but this agent may not retire it (no"
                  + " write permission). Retire or delete it in Agoora, or grant the agent write"
                  + " permission on its path.",
              dataPort.getDataPortId(),
              dataPort.getTopicName());
        }
        return Optional.empty();
      }
      LOG.error(
          "Error while updating data port in logistics (set state to deleted), will skip and continue.",
          e);
      return Optional.empty();
    }
    refusedRemovals.remove(dataPort.getDataPortId());
    LOG.info(
        "Inactivated data port with id '{}' and name '{}'", saved.getId(), saved.getName());
    return Optional.of(saved);
  }

  public Optional<DataSubscriptionState> deleteDataSubscriptionState(
      final KafkaConsumerGroup dataSubscriptionState) {
    if (dataSubscriptionState.getDataSubscriptionStateId() == null) {
      throw new IllegalStateException(
          "Got a KafkaConsumerGroup without a dataSubscriptionStateId.");
    }
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
      if (isRefused(e)) {
        if (refusedRemovals.add(dataSubscriptionState.getDataSubscriptionStateId())) {
          LOG.warn(
              "Data subscription state '{}' (consumer group '{}', topic '{}') is gone from Kafka but"
                  + " this agent may not retire it (no write permission). Retire or delete it in"
                  + " Agoora, or grant the agent write permission on its path.",
              dataSubscriptionState.getDataSubscriptionStateId(),
              dataSubscriptionState.getConsumerGroupName(),
              dataSubscriptionState.getTopicName());
        }
        return Optional.empty();
      }
      LOG.error(
          "Error while updating data subscription state in logistics, will skip and continue.", e);
      return Optional.empty();
    }
    refusedRemovals.remove(dataSubscriptionState.getDataSubscriptionStateId());
    LOG.info(
        "Inactivated data subscription state with id '{}' and name '{}'",
        saved.getId(),
        saved.getName());
    return Optional.of(saved);
  }

  /**
   * Logistics answers NOT_FOUND when the caller may not write an entity (to not leak its
   * existence), e.g. a data port in a path the agent has no write permission on.
   */
  private static boolean isRefused(StatusRuntimeException e) {
    return e.getStatus().getCode() == Status.Code.NOT_FOUND;
  }

  /** Ids whose removal was refused and not retried successfully since. Visible for tests. */
  Set<String> getRefusedRemovals() {
    return refusedRemovals;
  }
}
