package io.spoud.agoora.agents.kafka.service;

import io.spoud.agoora.agents.kafka.data.KafkaConsumerGroup;
import io.spoud.agoora.agents.kafka.data.KafkaConsumerGroupMapper;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import io.spoud.agoora.agents.kafka.data.KafkaTopicMapper;
import io.spoud.agoora.agents.kafka.kafka.KafkaAdminScrapper;
import io.spoud.agoora.agents.kafka.logistics.LogisticsService;
import io.spoud.agoora.agents.kafka.repository.KafkaConsumerGroupRepository;
import io.spoud.agoora.agents.kafka.repository.KafkaTopicRepository;
import io.spoud.agoora.agents.kafka.schema.SchemaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reconciles the Kafka cluster with logistics.
 *
 * <p>Removals are computed against what logistics currently lists as available for this agent's
 * transport, not only against the in-memory state fed by hooks. This way a removal that failed in
 * a previous iteration is retried, and removals also happen right after a restart, before the
 * hooks replay has filled the in-memory state. If logistics cannot be listed, the in-memory state
 * is used as before.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class DataService {
  private final KafkaAdminScrapper manager;

  private final KafkaTopicRepository kafkaTopicRepository;
  private final KafkaConsumerGroupRepository kafkaConsumerGroupRepository;
  private final KafkaTopicMapper kafkaTopicMapper;
  private final KafkaConsumerGroupMapper kafkaConsumerGroupMapper;

  private final LogisticsService logisticsService;
  private final SchemaService schemaService;

  public void updateTopics() {
    Map<String, KafkaTopic> localDataPorts =
        kafkaTopicRepository.getStates().stream()
            .collect(Collectors.toMap(KafkaTopic::getInternalId, Function.identity()));

    final List<KafkaTopic> topics = manager.getTopics();
    topics.forEach(
        topic -> {
          final KafkaTopic previous = localDataPorts.remove(topic.getInternalId());
          if (previous == null) {
            LOG.info("New topic found: {}", topic);
          }
          logisticsService.updateDataPort(topic).ifPresent(dp -> {
              topic.setDataPortId(dp.getId());
              schemaService.update(topic.getTopicName(), dp.getId());
          });
          kafkaTopicRepository.save(topic);
        });

    final Set<String> existing =
        topics.stream().map(KafkaTopic::getInternalId).collect(Collectors.toSet());
    final Collection<KafkaTopic> toRemove =
        logisticsService
            .listAvailableDataPorts()
            .map(
                dataPorts -> {
                  // logistics is the source of truth, local leftovers are already gone there
                  localDataPorts.values().forEach(kafkaTopicRepository::delete);
                  return dataPorts.stream()
                      .map(kafkaTopicMapper::create)
                      .flatMap(Optional::stream)
                      .filter(t -> !existing.contains(t.getInternalId()))
                      .collect(
                          Collectors.toMap(
                              KafkaTopic::getInternalId,
                              Function.identity(),
                              (a, b) -> a,
                              LinkedHashMap::new))
                      .values();
                })
            .orElse(localDataPorts.values());

    if (topics.isEmpty() && !toRemove.isEmpty()) {
      LOG.warn(
          "Kafka returned no topic but {} data ports are available in logistics. Skipping removals"
              + " to avoid deleting everything because of a temporary or permission issue.",
          toRemove.size());
      return;
    }

    toRemove.forEach(
        removed -> {
          LOG.info("Topic was removed: {}", removed);
          if (removed.getDataPortId() != null) {
            logisticsService.deleteDataPort(removed);
          }
          kafkaTopicRepository.delete(removed);
        });
  }

  public void updateConsumerGroups() {
    Map<String, KafkaConsumerGroup> localSubscriptionStates =
        kafkaConsumerGroupRepository.getStates().stream()
            .collect(Collectors.toMap(KafkaConsumerGroup::getInternalId, Function.identity()));

    final List<KafkaConsumerGroup> consumerGroups = manager.getConsumerGroups();
    consumerGroups.forEach(
        consumerGroup -> {
          final KafkaConsumerGroup previous =
              localSubscriptionStates.remove(consumerGroup.getInternalId());
          if (previous == null) {
            LOG.info("New consumerGroup found: {}", consumerGroup);
          }
          logisticsService
              .updateDataSubscriptionState(consumerGroup)
              .ifPresent(dp -> consumerGroup.setDataSubscriptionStateId(dp.getId()));
          kafkaConsumerGroupRepository.save(consumerGroup);
        });

    final Set<String> existing =
        consumerGroups.stream()
            .map(KafkaConsumerGroup::getInternalId)
            .collect(Collectors.toSet());
    final Collection<KafkaConsumerGroup> toRemove =
        logisticsService
            .listAvailableDataSubscriptionStates()
            .map(
                states -> {
                  localSubscriptionStates.values().forEach(kafkaConsumerGroupRepository::delete);
                  return states.stream()
                      .map(kafkaConsumerGroupMapper::create)
                      .flatMap(Optional::stream)
                      .filter(cg -> !existing.contains(cg.getInternalId()))
                      .collect(
                          Collectors.toMap(
                              KafkaConsumerGroup::getInternalId,
                              Function.identity(),
                              (a, b) -> a,
                              LinkedHashMap::new))
                      .values();
                })
            .orElse(localSubscriptionStates.values());

    toRemove.forEach(
        removed -> {
          LOG.info("ConsumerGroup was removed: {}", removed);
          if (removed.getDataSubscriptionStateId() != null) {
            logisticsService.deleteDataSubscriptionState(removed);
          }
          kafkaConsumerGroupRepository.delete(removed);
        });
  }
}
