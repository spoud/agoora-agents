package io.spoud.agoora.agents.kafka.metrics;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.MetricsClient;
import io.spoud.agoora.agents.kafka.data.KafkaConsumerGroup;
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
import java.time.Duration;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@QuarkusTest
class MetricsForwarderServiceTest {

  public static final String TOPIC_1 = "metric-topic1";
  public static final String TOPIC_2 = "metric-topic2";
  public static final String TOPIC_3 = "metric-topic3";
  public static final String TOPIC_4 = "metric-topic4";
  public static final String TOPIC_5 = "metric-topic5";
  public static final String GROUP_1 = "metric-group1";
  public static final String GROUP_2 = "metric-group2";
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
    kafkaTopicRepository.clear();
    kafkaConsumerGroupRepository.clear();
  }

  @Test
  void testScrapeMetricsTopic() {
    kafkaTopicRepository.save(KafkaTopic.builder().topicName(TOPIC_1).dataPortId("m-t-1").build());
    kafkaTopicRepository.save(KafkaTopic.builder().topicName(TOPIC_2).dataPortId("m-t-2").build());
    kafkaTopicRepository.save(KafkaTopic.builder().topicName(TOPIC_3).dataPortId("m-t-3").build());

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
        .updateMetric(eq("m-t-1"), eq(ResourceMetric.MetricType.DATA_PORT_MESSAGES), eq(10.0d));
    verify(metricsClient)
        .updateMetric(eq("m-t-2"), eq(ResourceMetric.MetricType.DATA_PORT_MESSAGES), eq(14.0d));
    verify(metricsClient)
        .updateMetric(eq("m-t-3"), eq(ResourceMetric.MetricType.DATA_PORT_MESSAGES), eq(0.0d));
  }

  @Test
  void testScrapeMetricsSubscription() {
    kafkaTopicRepository.save(KafkaTopic.builder().topicName(TOPIC_4).dataPortId("m-t-1").build());
    kafkaTopicRepository.save(KafkaTopic.builder().topicName(TOPIC_5).dataPortId("m-t-2").build());

    kafkaConsumerGroupRepository.save(
        KafkaConsumerGroup.builder()
            .topicName(TOPIC_4)
            .dataPortId("m-t-1")
            .consumerGroupName(GROUP_1)
            .dataSubscriptionStateId("m-g-1-1")
            .build());
    kafkaConsumerGroupRepository.save(
        KafkaConsumerGroup.builder()
            .topicName(TOPIC_5)
            .dataPortId("m-t-2")
            .consumerGroupName(GROUP_1)
            .dataSubscriptionStateId("m-g-1-2")
            .build());
    kafkaConsumerGroupRepository.save(
        KafkaConsumerGroup.builder()
            .topicName(TOPIC_5)
            .dataPortId("m-t-2")
            .consumerGroupName(GROUP_2)
            .dataSubscriptionStateId("m-g-2-2")
            .build());

    IntStream.range(0, 10)
        .forEach(
            i -> {
              kafkaUtils.produce(TOPIC_4, DATA);
            });
    IntStream.range(0, 14)
        .forEach(
            i -> {
              kafkaUtils.produce(TOPIC_5, DATA);
            });

    kafkaUtils.consume(TOPIC_4, GROUP_1, 10, Duration.ofSeconds(5));
    kafkaUtils.consume(TOPIC_5, GROUP_1, 14, Duration.ofSeconds(5));
    kafkaUtils.consume(TOPIC_5, GROUP_2, 14, Duration.ofSeconds(5));

    IntStream.range(0, 9)
        .forEach(
            i -> {
              kafkaUtils.produce(TOPIC_4, DATA);
            });
    IntStream.range(0, 12)
        .forEach(
            i -> {
              kafkaUtils.produce(TOPIC_5, DATA);
            });

    metricsForwarderService.scrapeMetrics();

    verify(metricsClient)
        .updateMetric(
            eq("m-g-1-1"),
            eq(ResourceMetric.MetricType.DATA_SUBSCRIPTION_STATE_MESSAGES),
            eq(10.0d));
    verify(metricsClient)
        .updateMetric(
            eq("m-g-1-1"),
            eq(ResourceMetric.MetricType.DATA_SUBSCRIPTION_STATE_MESSAGES_LAG),
            eq(9.0d));

    verify(metricsClient)
        .updateMetric(
            eq("m-g-1-2"),
            eq(ResourceMetric.MetricType.DATA_SUBSCRIPTION_STATE_MESSAGES),
            eq(14.0d));
    verify(metricsClient)
        .updateMetric(
            eq("m-g-1-2"),
            eq(ResourceMetric.MetricType.DATA_SUBSCRIPTION_STATE_MESSAGES_LAG),
            eq(12.0d));

    verify(metricsClient)
        .updateMetric(
            eq("m-g-2-2"),
            eq(ResourceMetric.MetricType.DATA_SUBSCRIPTION_STATE_MESSAGES),
            eq(14.0d));
    verify(metricsClient)
        .updateMetric(
            eq("m-g-2-2"),
            eq(ResourceMetric.MetricType.DATA_SUBSCRIPTION_STATE_MESSAGES_LAG),
            eq(12.0d));
  }
}
