package io.spoud.agoora.agents.kafka.repository;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.hooks.domain.v1.LogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class KafkaTopicRepositoryTest {
  @Inject KafkaTopicRepository kafkaTopicRepository;

  @AfterEach
  void tearDown() {
    kafkaTopicRepository.clear();
  }

  @Test
  void testWitoutDataPortId() {
    final KafkaTopic topic = KafkaTopic.builder().topicName("topic1").build();
    kafkaTopicRepository.save(topic);
    assertThat(kafkaTopicRepository.getStates()).hasSize(1);

    kafkaTopicRepository.delete(topic);
    assertThat(kafkaTopicRepository.getStates()).isEmpty();
  }

  @Test
  void testDeleteWitoutDataPortId() {
    kafkaTopicRepository.save(KafkaTopic.builder().topicName("topic1").dataPortId("hellooo").build());
    assertThat(kafkaTopicRepository.getStates()).hasSize(1);

    kafkaTopicRepository.delete(KafkaTopic.builder().topicName("topic1").build());
    assertThat(kafkaTopicRepository.getStates()).isEmpty();
  }

  @Test
  void testWrongLogEntry() {
    kafkaTopicRepository.onNext(
        LogRecord.newBuilder().setEntityType(ResourceEntity.Type.DATA_SUBSCRIPTION_STATE).build());
    assertThat(kafkaTopicRepository.getStates()).isEmpty();
  }
}
