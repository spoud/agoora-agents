package io.spoud.agoora.agents.kafka.kafka;

import io.quarkus.runtime.StartupEvent;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.utils.Bytes;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class KafkaTopicReader {
  public static final Duration TIMEOUT_PER_TOPIC = Duration.ofSeconds(10);
  private final KafkaAgentConfig config;
  private Consumer<Bytes, Bytes> globalConsumer;

  void startup(@Observes StartupEvent event) {
    LOG.debug("Staring kafka reader");
    globalConsumer = KafkaFactory.createConsumer(config);
  }

  public List<byte[]> getSamples(String topic) {
    Consumer<Bytes, Bytes> consumer = KafkaFactory.createConsumer(config);
    final Instant start = Instant.now();

    final Map<TopicPartition, Range> ranges = getRanges(topic, consumer);
    if (ranges.isEmpty()) {
      return Collections.emptyList();
    }

    consumer.assign(ranges.keySet());

    ranges
        .entrySet()
        .forEach(entry -> consumer.seek(entry.getKey(), entry.getValue().getBeginning()));

    List<byte[]> samples = new ArrayList<>();

    Map<Integer, Long> endOffsets =
        ranges.entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> entry.getKey().partition(), entry -> entry.getValue().getEnd()));

    final Set<Integer> runningPartitions = new TreeSet<>(endOffsets.keySet());

    while (!runningPartitions.isEmpty()) {
      if (Duration.between(start, Instant.now()).compareTo(TIMEOUT_PER_TOPIC) > 0) {
        LOG.warn("Timeout for topic {}, ranges={}", topic, ranges);
        break;
      }

      final ConsumerRecords<Bytes, Bytes> poll = consumer.poll(Duration.ofSeconds(10));

      poll.forEach(
          rec -> {
            final Long endOffset = endOffsets.get(rec.partition());
            if (rec.offset() >= endOffset) {
              // we reached the end, remove partition
              runningPartitions.remove(rec.partition());
            }
            if (rec.offset() <= endOffset) {
              if (rec.value() != null) {
                samples.add(rec.value().get());
              }
            }
          });
    }
    consumer.assign(Collections.emptyList());
    LOG.debug(
        "Topic '{}', partition count={}, samples count={}, duration={}, ranges={}",
        topic,
        ranges.keySet().size(),
        samples.size(),
        Duration.between(start, Instant.now()),
        ranges);

    consumer.close();
    return samples;
  }

  public Map<TopicPartition, Long> getEndOffsetByTopic(final String topic) {
    final List<TopicPartition> topics = getTopicPartitions(topic, globalConsumer);
    if (topics.isEmpty()) {
      return Collections.emptyMap();
    }
    return globalConsumer.endOffsets(topics);
  }

  private List<TopicPartition> getTopicPartitions(String topic, Consumer<Bytes, Bytes> consumer) {
    final List<PartitionInfo> partitions = consumer.partitionsFor(topic);
    if (partitions == null) {
      LOG.warn("No partition found for topic {}", topic);
      return Collections.emptyList();
    }
    return partitions.stream()
        .map(pi -> new TopicPartition(pi.topic(), pi.partition()))
        .collect(Collectors.toList());
  }

  private Map<TopicPartition, Range> getRanges(String topic, Consumer<Bytes, Bytes> consumer) {
    final List<TopicPartition> topicPartitions = getTopicPartitions(topic, consumer);
    if (topicPartitions.isEmpty()) {
      return Collections.emptyMap();
    }
    final Map<TopicPartition, Long> beginning = consumer.beginningOffsets(topicPartitions);
    final Map<TopicPartition, Long> end = consumer.endOffsets(topicPartitions);

    int samplesPerPartitions = config.scrapper().maxSamples() / end.size();

    return end.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                endEntry -> {
                  // all the -1 are because kafka give us the next offset
                  long start = Math.max(beginning.getOrDefault(endEntry.getKey(), 0L) - 1, 0L);
                  start = Math.max(start, endEntry.getValue() - samplesPerPartitions);
                  return new Range(start, endEntry.getValue() - 1);
                }))
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue().isValid())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Data
  @RequiredArgsConstructor
  public static class Range {
    private final long beginning;
    private final long end;

    private boolean isValid() {
      return end > beginning;
    }
  }
}
