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
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
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
  private Pattern topicFilterRegex;
  private Pattern consumerGroupFilterRegex;

  void postConstruct(@Observes StartupEvent event) {
    topicFilterRegex = Pattern.compile(config.kafka().topicFilterRegex());
    consumerGroupFilterRegex = Pattern.compile(config.kafka().consumerGroupFilterRegex());
    adminClient = KafkaFactory.createAdminClient(config);
  }

  public List<KafkaTopic> getTopics() {
    try {
      List<String> topicNames =
          adminClient.listTopics().names().get().stream()
              .filter(t -> !t.startsWith("_")) // remove internal topics
              .filter(t -> this.topicFilterRegex.matcher(t).matches())
              .collect(Collectors.toList());
      return adminClient.describeTopics(topicNames).all().get().values().stream()
          .map(
              topicDescription ->
                  kafkaTopicMapper.create(
                      topicDescription.name(), topicDescription.partitions().size()))
          .collect(Collectors.toList());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    } catch (ExecutionException e) {
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
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    } catch (ExecutionException e) {
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
          .map(TopicPartition::topic)
          .collect(Collectors.toSet());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    } catch (ExecutionException e) {
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
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    } catch (ExecutionException e) {
      throw new IllegalStateException(e);
    }
  }
}
