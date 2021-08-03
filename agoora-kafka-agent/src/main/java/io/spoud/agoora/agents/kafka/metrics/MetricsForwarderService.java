package io.spoud.agoora.agents.kafka.metrics;

import io.spoud.agoora.agents.kafka.kafka.KafkaAdminScrapper;
import io.spoud.agoora.agents.kafka.kafka.KafkaTopicReader;
import io.spoud.agoora.agents.kafka.repository.KafkaConsumerGroupRepository;
import io.spoud.agoora.agents.kafka.repository.KafkaTopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

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

    Map<String, Long> offerMetric = scrapeDataPortMessagesMetrics();

    // data subscription state messages
    scrapeDataSubscriptionStateMessagesMetrics(offerMetric);
  }

  public Map<String, Long> scrapeDataPortMessagesMetrics() {
    Map<String, Long> dataPortsMetrics = new HashMap<>();
    LOG.info("Metrics scraping for topics");
    kafkaTopicRepository
        .getStates()
        .forEach(
            dataPort -> {
              try {
                String dataPortId = dataPort.getDataPortId();
                String topicName = dataPort.getTopicName();

                if (dataPortId != null) {
                  final long offsetSum =
                      kafkaTopicReader.getEndOffsetByTopic(topicName).values().stream()
                          .mapToLong(offset -> offset)
                          .sum();
                  final long now = System.currentTimeMillis();
                  LOG.debug(
                      "Offset for topic '{}' is {}. it will be forwarded.",
                      topicName,
                      now,
                      offsetSum);

                  dataPortsMetrics.put(topicName, offsetSum);

                  lookerService.updateMetrics(
                      dataPortId, MetricsType.DATA_PORT_MESSAGES_COUNT, (double) offsetSum);

                } else {
                  LOG.warn("No data port id for topic {}, cannot upload metrics", topicName);
                }
              } catch (Exception ex) {
                LOG.warn("Unable to forward metrics for data port", ex);
              }
            });
    LOG.info("Metrics scraping DONE.");
    return dataPortsMetrics;
  }

  public void scrapeDataSubscriptionStateMessagesMetrics(Map<String, Long> offsetPerTopic) {
    LOG.info("Metrics scraping for consumer group ");

    kafkaConsumerGroupRepository
        .getStates()
        .forEach(
            dataSubscriptionState -> {
              try {
                String id = dataSubscriptionState.getDataSubscriptionStateId();
                String consumerGroup = dataSubscriptionState.getConsumerGroupName();
                String topicName = dataSubscriptionState.getTopicName();

                final Map<TopicPartition, OffsetAndMetadata> offsetByConsumerGroup =
                    kafkaService.getOffsetByConsumerGroup(consumerGroup);

                final long consumerOffsetSum =
                    offsetByConsumerGroup.entrySet().stream()
                        .filter(e -> e.getKey().topic().equals(topicName))
                        .map(Map.Entry::getValue)
                        .mapToLong(OffsetAndMetadata::offset)
                        .sum();

                lookerService.updateMetrics(
                    id,
                    MetricsType.DATA_SUBSCRIPTION_STATE_MESSAGES_COUNT,
                    (double) consumerOffsetSum);

                Optional.ofNullable(offsetPerTopic.get(topicName))
                    .map(topicOffset -> topicOffset - consumerOffsetSum)
                    .ifPresent(
                        lag ->
                            lookerService.updateMetrics(
                                id,
                                MetricsType.DATA_SUBSCRIPTION_STATE_MESSAGES_LAG_COUNT,
                                (double) lag));
              } catch (Exception ex) {
                LOG.warn("Unable to forward metrics for data subscription state", ex);
              }
            });
  }
}
