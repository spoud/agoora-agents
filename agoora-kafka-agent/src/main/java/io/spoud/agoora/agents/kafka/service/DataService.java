package io.spoud.agoora.agents.kafka.service;

import io.spoud.agoora.agents.kafka.data.KafkaConsumerGroup;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import io.spoud.agoora.agents.kafka.kafka.KafkaAdminScrapper;
import io.spoud.agoora.agents.kafka.logistics.LogisticsService;
import io.spoud.agoora.agents.kafka.repository.KafkaConsumerGroupRepository;
import io.spoud.agoora.agents.kafka.repository.KafkaTopicRepository;
import io.spoud.agoora.agents.kafka.schema.SchemaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class DataService {
  private final KafkaAdminScrapper manager;

  private final KafkaTopicRepository kafkaTopicRepository;
  private final KafkaConsumerGroupRepository kafkaConsumerGroupRepository;

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

    localDataPorts
        .values()
        .forEach(
            toRemove -> {
              LOG.info("Topic was removed: {}", toRemove);
              logisticsService.deleteDataPort(toRemove);
              kafkaTopicRepository.delete(toRemove);
            });

    // TODO schema
    // TODO update metrics
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

    localSubscriptionStates
        .values()
        .forEach(
            toRemove -> {
              LOG.info("ConsumerGroup was removed: {}", toRemove);
              logisticsService.deleteDataSubscriptionState(toRemove);
              kafkaConsumerGroupRepository.delete(toRemove);
            });

    // TODO update metrics
  }

  public void profileData() {
    LOG.error("Profiling not implemented yet");
  }
}
