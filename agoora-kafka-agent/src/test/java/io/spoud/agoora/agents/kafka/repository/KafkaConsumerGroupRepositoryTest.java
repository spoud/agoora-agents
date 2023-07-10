package io.spoud.agoora.agents.kafka.repository;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.kafka.data.KafkaConsumerGroup;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.hooks.domain.v1.LogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class KafkaConsumerGroupRepositoryTest {
  @Inject KafkaTopicRepository kafkaTopicRepository;
  @Inject KafkaConsumerGroupRepository kafkaConsumerGroupRepository;

  @BeforeEach
  void setup() {
    kafkaTopicRepository.save(KafkaTopic.builder().topicName("topic1").dataPortId("dp1").build());
  }

  @AfterEach
  void tearDown() {
    kafkaConsumerGroupRepository.clear();
  }

  @Test
  void testWitoutDataSubscriptionStateId() {
    final KafkaConsumerGroup topic =
        KafkaConsumerGroup.builder().consumerGroupName("topic1").dataPortId("dp1").build();
    kafkaConsumerGroupRepository.save(topic);
    assertThat(kafkaConsumerGroupRepository.getStates()).hasSize(1);

    kafkaConsumerGroupRepository.delete(topic);
    assertThat(kafkaConsumerGroupRepository.getStates()).isEmpty();
  }

  @Test
  void testDeleteWitoutDataSubscriptionStateId() {
    kafkaConsumerGroupRepository.save(
        KafkaConsumerGroup.builder()
            .consumerGroupName("topic1")
            .dataSubscriptionStateId("dss1")
            .dataPortId("dp1")
            .build());
    assertThat(kafkaConsumerGroupRepository.getStates()).hasSize(1);

    kafkaConsumerGroupRepository.delete(
        KafkaConsumerGroup.builder().consumerGroupName("topic1").dataPortId("dp1").build());
    assertThat(kafkaConsumerGroupRepository.getStates()).isEmpty();
  }

  @Test
  void testWrongLogEntry() {
    kafkaConsumerGroupRepository.onNext(
        LogRecord.newBuilder().setEntityType(ResourceEntity.Type.DATA_PORT).build());
    assertThat(kafkaConsumerGroupRepository.getStates()).isEmpty();
  }
}
