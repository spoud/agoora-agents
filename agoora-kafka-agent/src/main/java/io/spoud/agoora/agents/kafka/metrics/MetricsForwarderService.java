package io.spoud.agoora.agents.kafka.metrics;

import io.spoud.agoora.agents.kafka.kafka.KafkaAdminScrapper;
import io.spoud.agoora.agents.kafka.kafka.KafkaTopicReader;
import io.spoud.agoora.agents.kafka.metrics.model.MetricValue;
import io.spoud.agoora.agents.kafka.repository.KafkaConsumerGroupRepository;
import io.spoud.agoora.agents.kafka.repository.KafkaTopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class MetricsForwarderService {

  private final KafkaTopicRepository kafkaTopicRepository;
  private final KafkaConsumerGroupRepository kafkaConsumerGroupRepository;
  private final LookerMetricsService lookerService;
  private final KafkaAdminScrapper kafkaService;
  private final KafkaTopicReader kafkaTopicReader;

  public void scrapeMetrics() {
    // TODO to improve metric accuracry we should scrape a topic and then it's consumer groups
    // (directly). And not do all topics and then all consumer groups

    Map<String, Map<Integer, MetricValue>> offerMetric = scrapeDataPortMessagesMetrics();

    // data subscription state messages
    scrapeDataSubscriptionStateMessagesMetrics(offerMetric);
  }

  public Map<String, Map<Integer, MetricValue>> scrapeDataPortMessagesMetrics() {
    Map<String, Map<Integer, MetricValue>> dataPortsMetrics = new HashMap<>();
    MetricsType type = MetricsType.DATA_PORT_MESSAGES_COUNT;
    LOG.info("Metrics scraping for {}", type);
    kafkaTopicRepository
        .getStates()
        .forEach(
            dataPort -> {
              String dataPortId = dataPort.getDataPortId();
              String topicName = dataPort.getTopicName();
              HashMap<Integer, MetricValue> topicMetric = new HashMap<>();

              if (dataPortId != null) {
                dataPortsMetrics.put(topicName, topicMetric);
                kafkaTopicReader
                    .getEndOffsetByTopic(topicName)
                    .forEach(
                        (topicPartition, offset) -> {
                          final MetricValue metricValue =
                              new MetricValue(System.currentTimeMillis(), offset);
                          LOG.debug(
                              "Got {} metrics for topic '{}' with timestamp {} and value {}. Will be forwarded.",
                              type,
                              topicPartition,
                              metricValue.getTimestamp(),
                              metricValue.getValue());
                          topicMetric.put(topicPartition.partition(), metricValue);
                          lookerService.updateMetrics(
                              dataPortId,
                              type,
                              metricValue.getValue(),
                              Map.of("partition", String.valueOf(topicPartition.partition())));
                        });

              } else {
                LOG.warn("No data port id for topic {}, cannot upload metrics", topicName);
              }
            });
    LOG.info("Metrics scraping DONE {}.", type);
    return dataPortsMetrics;
  }

  public void scrapeDataSubscriptionStateMessagesMetrics(
      Map<String, Map<Integer, MetricValue>> dataOfferMetrics) {

    MetricsType type = MetricsType.DATA_SUBSCRIPTION_STATE_MESSAGES_COUNT;
    MetricsType lagType = MetricsType.DATA_SUBSCRIPTION_STATE_MESSAGES_LAG_COUNT;
    LOG.info("Metrics scraping for {}", type);

    kafkaConsumerGroupRepository
        .getStates()
        .forEach(
            dataSubscriptionState -> {
              String id = dataSubscriptionState.getDataSubscriptionStateId();
              String consumerGroup = dataSubscriptionState.getConsumerGroupName();
              String topicName = dataSubscriptionState.getTopicName();
              kafkaService
                  .getOffsetByConsumerGroup(consumerGroup)
                  .forEach(
                      (topicPartition, offsetAndMetadata) -> {
                        // filter out non related consumer group topics
                        if (!topicPartition.topic().equals(topicName)) {
                          return;
                        }
                        int partition = topicPartition.partition();

                        final MetricValue metricValue =
                            new MetricValue(System.currentTimeMillis(), offsetAndMetadata.offset());
                        LOG.debug(
                            "Got {} metrics for consumerGroup '{}' with timestamp {} and value {}. Will be forwarded.",
                            type,
                            consumerGroup,
                            metricValue.getValue());
                        lookerService.updateMetrics(
                            id,
                            type,
                            metricValue.getValue(),
                            Map.of("partition", String.valueOf(partition)));

                        Optional.ofNullable(dataOfferMetrics.get(topicName))
                            .map(m -> m.get(partition))
                            .map(MetricValue::getValue)
                            .ifPresentOrElse(
                                dataOfferOffset -> {
                                  final MetricValue lagMetricValue =
                                      new MetricValue(
                                          System.currentTimeMillis(),
                                          dataOfferOffset - offsetAndMetadata.offset());
                                  LOG.debug(
                                      "Got {} metrics for consumerGroup '{}' with timestamp {} and value {}. Will be forwarded.",
                                      lagType,
                                      consumerGroup,
                                      lagMetricValue.getValue());
                                  lookerService.updateMetrics(
                                      id,
                                      lagType,
                                      lagMetricValue.getValue(),
                                      Map.of("partition", String.valueOf(partition)));
                                },
                                () -> {
                                  LOG.warn(
                                      "No data offer metric to compute lag for topic='{}', consumer-group='{}', partition='{}'",
                                      topicName,
                                      consumerGroup,
                                      partition);
                                });
                      });
            });
  }
}
