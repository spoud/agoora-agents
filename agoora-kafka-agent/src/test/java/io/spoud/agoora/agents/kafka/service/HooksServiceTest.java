package io.spoud.agoora.agents.kafka.service;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.HooksClient;
import io.spoud.agoora.agents.kafka.Constants;
import io.spoud.agoora.agents.kafka.data.KafkaConsumerGroup;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import io.spoud.agoora.agents.kafka.repository.KafkaConsumerGroupRepository;
import io.spoud.agoora.agents.kafka.repository.KafkaTopicRepository;
import io.spoud.agoora.agents.test.mock.HooksClientMockProvider;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.hooks.domain.v1.LogRecord;
import io.spoud.sdm.hooks.domain.v1.StateChangeAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;

@QuarkusTest
class HooksServiceTest {

  @Inject KafkaTopicRepository kafkaTopicRepository;
  @Inject KafkaConsumerGroupRepository kafkaConsumerGroupRepository;

  @Inject HooksClient hooksClient;

  @Inject HooksService hooksService;

  @AfterEach
  public void tearDown() {
    reset(hooksClient);
    kafkaTopicRepository.clear();
    kafkaConsumerGroupRepository.clear();
  }

  @Test
  void testHooks() {
    String port1 = UUID.randomUUID().toString();
    String port2 = UUID.randomUUID().toString();
    String port3 = UUID.randomUUID().toString();
    String sub1 = UUID.randomUUID().toString();
    String sub2 = UUID.randomUUID().toString();
    List<LogRecord> records =
        Arrays.asList(
            HooksClientMockProvider.generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED,
                port1,
                "port1",
                "/path/",
                Map.of(Constants.SDM_PROPERTIES_KAFKA_TOPIC, "topic1")),
            HooksClientMockProvider.generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED,
                port2,
                "port2",
                "/path/",
                Map.of(Constants.SDM_PROPERTIES_KAFKA_TOPIC, "topic2")),
            HooksClientMockProvider.generateDataSubscriptionStateLogRecord(
                StateChangeAction.Type.UPDATED,
                sub1,
                port2,
                "sub1",
                "/path/",
                Map.of(
                    Constants.SDM_PROPERTIES_KAFKA_TOPIC, "topic2",
                    Constants.SDM_PROPERTIES_KAFKA_CONSUMER_GROUP, "group1")),
            HooksClientMockProvider.generateDataSubscriptionStateLogRecord(
                StateChangeAction.Type.UPDATED,
                sub2,
                port2,
                "sub2",
                "/path/",
                Map.of(
                    Constants.SDM_PROPERTIES_KAFKA_TOPIC, "topic2",
                    Constants.SDM_PROPERTIES_KAFKA_CONSUMER_GROUP, "group2")),
            HooksClientMockProvider.generateDataSubscriptionStateLogRecord(
                StateChangeAction.Type.DELETED,
                sub2,
                port2,
                "sub2",
                "/path/",
                Map.of(
                    Constants.SDM_PROPERTIES_KAFKA_TOPIC, "topic2",
                    Constants.SDM_PROPERTIES_KAFKA_CONSUMER_GROUP, "group2")),
            HooksClientMockProvider.generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED,
                port2,
                "port2-edited",
                "/path/",
                Map.of(Constants.SDM_PROPERTIES_KAFKA_TOPIC, "topic2")),
            HooksClientMockProvider.generateDataPortLogRecord(
                StateChangeAction.Type.DELETED,
                port1,
                "port1",
                "/path/",
                Map.of(Constants.SDM_PROPERTIES_KAFKA_TOPIC, "topic1")),
            HooksClientMockProvider.generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED,
                port3,
                "port3",
                "/path/",
                Map.of(Constants.SDM_PROPERTIES_KAFKA_TOPIC, "topic3")));
    HooksClientMockProvider.withLogRecord(hooksClient, records);
    hooksService.startListeningToHooks();

    final Collection<KafkaTopic> topics = kafkaTopicRepository.getStates();
    final Collection<KafkaConsumerGroup> consumerGroups = kafkaConsumerGroupRepository.getStates();

    assertThat(topics)
        .extracting(KafkaTopic::getDataPortId)
        .containsExactlyInAnyOrder(port2, port3);
    assertThat(topics)
        .extracting(KafkaTopic::getTopicName)
        .containsExactlyInAnyOrder("topic2", "topic3");
    assertThat(consumerGroups)
        .extracting(KafkaConsumerGroup::getDataSubscriptionStateId)
        .containsExactlyInAnyOrder(sub1);
    assertThat(consumerGroups)
        .extracting(KafkaConsumerGroup::getConsumerGroupName)
        .containsExactlyInAnyOrder("group1");
  }

  @Test
  public void testPortWithoutTopic() {
    List<LogRecord> records =
        Arrays.asList(
            HooksClientMockProvider.generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED,
                UUID.randomUUID().toString(),
                "port1",
                "/path/",
                Map.of(Constants.SDM_PROPERTIES_KAFKA_TOPIC, "topic1")),
            HooksClientMockProvider.generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED, UUID.randomUUID().toString(), "port2", "/path/"));
    HooksClientMockProvider.withLogRecord(hooksClient, records);
    hooksService.startListeningToHooks();

    assertThat(kafkaTopicRepository.getStates()).hasSize(1);
  }

  @Test
  public void testSubWithoutGroup() {
    final String dataPortId = UUID.randomUUID().toString();

    kafkaTopicRepository.save(
        KafkaTopic.builder().dataPortId(dataPortId).topicName("topic1").build());

    List<LogRecord> records =
        Arrays.asList(
            HooksClientMockProvider.generateDataSubscriptionStateLogRecord(
                StateChangeAction.Type.UPDATED,
                UUID.randomUUID().toString(),
                dataPortId,
                "sub1",
                "/path/",
                Map.of(
                    Constants.SDM_PROPERTIES_KAFKA_CONSUMER_GROUP, "group1",
                    Constants.SDM_PROPERTIES_KAFKA_TOPIC, "topic1")),
            HooksClientMockProvider.generateDataSubscriptionStateLogRecord(
                StateChangeAction.Type.UPDATED,
                UUID.randomUUID().toString(),
                dataPortId,
                "port2",
                "/path/",
                Map.of(Constants.SDM_PROPERTIES_KAFKA_TOPIC, "topic1")));
    HooksClientMockProvider.withLogRecord(hooksClient, records);
    hooksService.startListeningToHooks();

    assertThat(kafkaConsumerGroupRepository.getStates()).hasSize(1);
  }

  @Test
  void testDifferentTypeOfLotEntry() {
    final String dataPortId = UUID.randomUUID().toString();
    List<LogRecord> records =
        Arrays.asList(
            HooksClientMockProvider.generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED,
                dataPortId,
                "port1",
                "/path/",
                Map.of(Constants.SDM_PROPERTIES_KAFKA_TOPIC, "topic1")),
            HooksClientMockProvider.generateDataSubscriptionStateLogRecord(
                StateChangeAction.Type.UPDATED,
                UUID.randomUUID().toString(),
                dataPortId,
                "port2",
                "/path/",
                Map.of(
                    Constants.SDM_PROPERTIES_KAFKA_CONSUMER_GROUP,
                    "group1",
                    Constants.SDM_PROPERTIES_KAFKA_TOPIC,
                    "topic1")),
            LogRecord.newBuilder().setEntityType(ResourceEntity.Type.CHECKPOINT).build(),
            LogRecord.newBuilder().setEntityType(ResourceEntity.Type.DATA_ITEM).build(),
            LogRecord.newBuilder().setEntityType(ResourceEntity.Type.DATA_OFFER).build(),
            LogRecord.newBuilder().setEntityType(ResourceEntity.Type.SCHEMA).build());
    HooksClientMockProvider.withLogRecord(hooksClient, records);
    hooksService.startListeningToHooks();

    // other should just be ignored
    assertThat(kafkaTopicRepository.getStates()).hasSize(1);
    assertThat(kafkaConsumerGroupRepository.getStates()).hasSize(1);
  }
}
