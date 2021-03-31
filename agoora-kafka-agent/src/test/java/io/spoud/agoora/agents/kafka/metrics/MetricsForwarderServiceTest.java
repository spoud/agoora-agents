package io.spoud.agoora.agents.kafka.metrics;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.MetricsClient;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import io.spoud.agoora.agents.kafka.repository.KafkaConsumerGroupRepository;
import io.spoud.agoora.agents.kafka.repository.KafkaTopicRepository;
import io.spoud.agoora.agents.kafka.utils.KafkaUtils;
import io.spoud.agoora.agents.test.mock.MetricsClientMockProvider;
import io.spoud.sdm.looker.v1alpha1.ResourceMetric;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@QuarkusTest
class MetricsForwarderServiceTest {

  public static final String TOPIC_1 = "metric-topic1";
  public static final String TOPIC_2 = "metric-topic2";
  public static final byte[] DATA = new byte[] {0x01, 0x02};
  @Inject MetricsClient metricsClient;
  @Inject MetricsForwarderService metricsForwarderService;
  @Inject KafkaTopicRepository kafkaTopicRepository;
  @Inject KafkaConsumerGroupRepository kafkaConsumerGroupRepository;
  @Inject KafkaUtils kafkaUtils;

  @BeforeEach
  void setup() {
    MetricsClientMockProvider.defaultMock(metricsClient);
  }

  @AfterEach
  void tearDown() {
    kafkaTopicRepository.getStates().forEach(kafkaTopicRepository::delete);
    kafkaConsumerGroupRepository.getStates().forEach(kafkaConsumerGroupRepository::delete);
  }

  @Test
  void testScrapeMetricsTopic() {
    kafkaTopicRepository.save(KafkaTopic.builder().topicName(TOPIC_1).dataPortId("m-t-1").build());
    kafkaTopicRepository.save(KafkaTopic.builder().topicName(TOPIC_2).dataPortId("m-t-2").build());

    IntStream.range(0, 10)
        .forEach(
            i -> {
              kafkaUtils.produce(TOPIC_1, DATA);
            });
    IntStream.range(0, 14)
        .forEach(
            i -> {
              kafkaUtils.produce(TOPIC_2, DATA);
            });

    metricsForwarderService.scrapeMetrics();

    verify(metricsClient)
        .updateMetric(
            ("m-t-1"), eq(ResourceMetric.MetricType.DATA_PORT_MESSAGES), eq(10.0d), anyMap());
    verify(metricsClient)
        .updateMetric(
            eq("m-t-2"), eq(ResourceMetric.MetricType.DATA_PORT_MESSAGES), eq(14.0d), anyMap());
  }
}
