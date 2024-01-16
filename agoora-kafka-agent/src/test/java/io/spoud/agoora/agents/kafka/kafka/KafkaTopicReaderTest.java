package io.spoud.agoora.agents.kafka.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.agoora.agents.kafka.config.data.ScrapperConfig;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class KafkaTopicReaderTest {

  public static final String TOPIC_NAME = "topic_name";
  KafkaConsumer mockedConsumer;
  MockedStatic<KafkaFactory> kafkaFactory;
  KafkaTopicReader kafkaTopicReader;

  @BeforeEach
  void setUp() {
    final KafkaAgentConfig config = mock(KafkaAgentConfig.class);
    final ScrapperConfig scrapperConfig = mock(ScrapperConfig.class);
    when(config.scrapper()).thenReturn(scrapperConfig);
    when(scrapperConfig.maxSamples()).thenReturn(100);

    kafkaFactory = mockStatic(KafkaFactory.class);
    mockedConsumer = mock(KafkaConsumer.class);
    kafkaFactory.when(() -> KafkaFactory.createConsumer(any())).thenReturn(mockedConsumer);
    when(mockedConsumer.partitionsFor(any()))
        .thenReturn(List.of(new PartitionInfo(TOPIC_NAME, 1, null, null, null)));
    final TopicPartition topicPartition = new TopicPartition(TOPIC_NAME, 0);
    when(mockedConsumer.beginningOffsets(any())).thenReturn(Map.of(topicPartition, 10L));
    when(mockedConsumer.endOffsets(any())).thenReturn(Map.of(topicPartition, 20L));
    when(mockedConsumer.poll(any()))
        .thenReturn(
            new ConsumerRecords(
                Map.of(
                    topicPartition,
                    List.of(new ConsumerRecord<>(TOPIC_NAME, 0, 20L, null, new byte[0])))));
    kafkaTopicReader = new KafkaTopicReader(config);
  }

  @AfterEach
  void tearDown() {
    try {
      // verify that no matter what, the consumer should have been closed
      verify(mockedConsumer).close();
    } finally {
      // cleanup static mock
      kafkaFactory.close();
    }
  }

  @Test
  void test_consumer_no_error() {
    kafkaTopicReader.getSamples(TOPIC_NAME);
  }

  @Test
  void test_with_read_error() {
    when(mockedConsumer.poll(any())).thenThrow(new KafkaException("Error when reading"));
    assertThatThrownBy(() -> kafkaTopicReader.getSamples(TOPIC_NAME))
        .isInstanceOf(KafkaException.class);
  }
}
