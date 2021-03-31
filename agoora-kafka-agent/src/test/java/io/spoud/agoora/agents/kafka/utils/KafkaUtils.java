package io.spoud.agoora.agents.kafka.utils;

import io.quarkus.runtime.StartupEvent;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.agoora.agents.kafka.kafka.KafkaFactory;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.utils.Bytes;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class KafkaUtils {

  private final KafkaAgentConfig kafkaAgentConfig;
  private KafkaProducer<Bytes, Bytes> producer;
  private Map<String, KafkaConsumer<Bytes, Bytes>> consumers = new ConcurrentHashMap<>();
  private AdminClient adminClient;

  void startup(@Observes StartupEvent event) {
    producer = KafkaFactory.createProducer(kafkaAgentConfig);
    adminClient = KafkaFactory.createAdminClient(kafkaAgentConfig);
  }

  @SneakyThrows
  public RecordMetadata produce(String topic, byte[] data) {
    ProducerRecord<Bytes, Bytes> record = new ProducerRecord<>(topic, Bytes.wrap(data));
    final RecordMetadata recordMetadata = producer.send(record).get();
    LOG.trace(
        "Produced record for topic {} at partition {} and offset {}",
        topic,
        recordMetadata.partition(),
        recordMetadata.offset());
    return recordMetadata;
  }

  public List<byte[]> consume(String topic, String consumerGroup, int count, Duration timeout) {
    final KafkaConsumer<Bytes, Bytes> consumer =
        consumers.computeIfAbsent(
            consumerGroup, name -> KafkaFactory.createConsumer(kafkaAgentConfig, name));
    consumer.subscribe(Collections.singleton(topic));
    final Instant start = Instant.now();
    List<byte[]> messages = new ArrayList<>(count);
    while (messages.size() < count) {
      if (Duration.between(start, Instant.now()).compareTo(timeout) > 0) {
        throw new IllegalStateException(
            "Timeout while reading topic "
                + topic
                + ". We only got "
                + messages.size()
                + "/"
                + count
                + " messages after "
                + timeout);
      }
      final ConsumerRecords<Bytes, Bytes> poll = consumer.poll(Duration.ofSeconds(1));
      poll.forEach(rec -> messages.add(rec.value().get()));
      consumer.commitSync(Duration.ofSeconds(2));
    }
    consumer.unsubscribe();
    return messages;
  }

  @SneakyThrows
  public void cleanup() {
    adminClient.deleteTopics(getExistingTopics()).all().get();

    // wait for the actual deletion
    await()
        .atMost(Duration.ofSeconds(60))
        .pollDelay(Duration.ofSeconds(1))
        .until(() -> getExistingTopics().isEmpty());
  }

  @SneakyThrows
  private List<String> getExistingTopics() {
    return adminClient.listTopics().listings().get().stream()
        .map(TopicListing::name)
        .filter(n -> !n.startsWith("_")) // avoid removing internal topics
        .collect(Collectors.toList());
  }
}
