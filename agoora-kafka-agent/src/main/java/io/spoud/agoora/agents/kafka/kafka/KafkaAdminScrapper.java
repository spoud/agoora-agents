package io.spoud.agoora.agents.kafka.kafka;

import io.quarkus.runtime.StartupEvent;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.agoora.agents.kafka.data.KafkaConsumerGroup;
import io.spoud.agoora.agents.kafka.data.KafkaConsumerGroupMapper;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import io.spoud.agoora.agents.kafka.data.KafkaTopicMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class KafkaAdminScrapper {

  private final KafkaAgentConfig config;
  private final KafkaTopicMapper kafkaTopicMapper;
  private final KafkaConsumerGroupMapper kafkaConsumerGroupMapper;
  private AdminClient adminClient;
  private Consumer<byte[], byte[]> consumer;
  private Pattern t;
  private Pattern consumerGroupFilterRegex;

  void postConstruct(@Observes StartupEvent event) {
    t = Pattern.compile(config.getKafka().getTopicFilterRegex());
    consumerGroupFilterRegex = Pattern.compile(config.getKafka().getConsumerGroupFilterRegex());
    adminClient = KafkaFactory.createAdminClient(config);
    consumer = KafkaFactory.createConsumer(config);
  }

  public List<KafkaTopic> getTopics() {
    try {
      List<String> topicNames =
          adminClient.listTopics().names().get().stream()
              .filter(t -> this.t.matcher(t).matches())
              .collect(Collectors.toList());
      return adminClient.describeTopics(topicNames).all().get().values().stream()
          .map(
              topicDescription ->
                  kafkaTopicMapper.create(
                      topicDescription.name(), topicDescription.partitions().size()))
          .collect(Collectors.toList());
    } catch (final InterruptedException | ExecutionException e) {
      throw new IllegalStateException(e);
    }
  }

  public List<KafkaConsumerGroup> getConsumerGroups() {
    try {
      return adminClient.listConsumerGroups().all().get().stream()
          .filter(cg -> !cg.isSimpleConsumerGroup())
          .map(ConsumerGroupListing::groupId)
          .filter(cg -> consumerGroupFilterRegex.matcher(cg).matches())
          .flatMap(
              consumerGroup ->
                  getTopicByConsumerGroup(consumerGroup).stream()
                      .map(topic -> kafkaConsumerGroupMapper.create(consumerGroup, topic)))
          .collect(Collectors.toList());
    } catch (final InterruptedException | ExecutionException e) {
      throw new IllegalStateException(e);
    }
  }

  private Set<String> getTopicByConsumerGroup(final String consumerGroup) {
    try {
      return adminClient
          .listConsumerGroupOffsets(consumerGroup)
          .partitionsToOffsetAndMetadata()
          .get()
          .keySet()
          .stream()
          .map(t -> t.topic())
          .collect(Collectors.toSet());
    } catch (final InterruptedException | ExecutionException e) {
      throw new IllegalStateException(e);
    }
  }

  public Map<TopicPartition, OffsetAndMetadata> getOffsetByConsumerGroup(
      final String consumerGroup) {
    try {
      return adminClient
          .listConsumerGroupOffsets(consumerGroup)
          .partitionsToOffsetAndMetadata()
          .get();
    } catch (final InterruptedException | ExecutionException e) {
      throw new IllegalStateException(e);
    }
  }

  public Map<TopicPartition, Long> getEndOffsetByTopic(final String topic) {
    final List<PartitionInfo> partitions = consumer.partitionsFor(topic);
    if (partitions == null) {
      LOG.warn("No partition found for topic {}", topic);
      return Collections.emptyMap();
    }
    final List<TopicPartition> topics =
        partitions.stream()
            .map(pi -> new TopicPartition(pi.topic(), pi.partition()))
            .collect(Collectors.toList());

    return consumer.endOffsets(topics);
  }
}