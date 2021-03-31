package io.spoud.agoora.agents.kafka.kafka;

import io.quarkus.runtime.configuration.ProfileManager;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.agoora.agents.kafka.config.data.KafkaConfig;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.utils.Bytes;

import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class KafkaFactory {

  public static final String PROFILE_CONFLUENT_CLOUD = "ccloud"; // TODO remove
  public static final int MAX_POOL_RECORD = 100;
  public static final String DEFAULT_CONSUMER_GROUP_NAME = "agoora-kafka-agent";

  public static AdminClient createAdminClient(KafkaAgentConfig kafkaAgentConfig) {
    return AdminClient.create(getAdminProperties(kafkaAgentConfig));
  }

  private static Map<String, Object> toMap(Properties props) {
    LOG.debug("Kafka properties:");
    props.forEach((k, v) -> LOG.debug("\t{}='{}'", k, v));

    return props.entrySet().stream()
        .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
  }

  private static Map<String, Object> getAdminProperties(KafkaAgentConfig kafkaAgentConfig) {
    return toMap(getCommonProperties(kafkaAgentConfig)).entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static Properties getCommonProperties(KafkaAgentConfig kafkaAgentConfig) {
    final Properties props = new Properties();
    final KafkaConfig kafkaConfig = kafkaAgentConfig.getKafka();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServers());

    // fix weird path (/tmp/tomcat-docbase.5741419017537392227.8080/...)
    kafkaConfig.getTrustStoreLocation().ifPresent(v -> props.put("ssl.truststore.location", v));
    kafkaConfig.getTrustStorePassword().ifPresent(v -> props.put("ssl.truststore.password", v));
    kafkaConfig.getKeyStoreLocation().ifPresent(v -> props.put("ssl.keystore.location", v));
    kafkaConfig.getKeyStorePassword().ifPresent(v -> props.put("ssl.keystore.password", v));

    // TODO more generic
    // TODO remove ccloud
    // TODO add truststore and keystore

    if (PROFILE_CONFLUENT_CLOUD.equals(ProfileManager.getActiveProfile())) {
      LOG.debug("Use confluent configuration");
      // confluent cloud specific
      props.put("security.protocol", "SASL_SSL");
      props.put(
          "sasl.jaas.config",
          "org.apache.kafka.common.security.plain.PlainLoginModule   required username=\""
              + kafkaConfig.getLoginKey().orElse("")
              + "\"   password=\""
              + kafkaConfig.getLoginSecret().orElse("")
              + "\";");
      props.put("ssl.endpoint.identification.algorithm", "https");
      props.put("sasl.mechanism", "PLAIN");
    }

    return props;
  }

  private static Properties getConsumerProperties(
      KafkaAgentConfig kafkaAgentConfig, String consumerGroupName) {
    final Properties props = getCommonProperties(kafkaAgentConfig);

    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, BytesDeserializer.class.getName());
    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, MAX_POOL_RECORD);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupName);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    return props;
  }

  public static KafkaConsumer<Bytes, Bytes> createConsumer(KafkaAgentConfig kafkaAgentConfig) {
    return createConsumer(kafkaAgentConfig, DEFAULT_CONSUMER_GROUP_NAME);
  }

  public static KafkaConsumer<Bytes, Bytes> createConsumer(
      KafkaAgentConfig kafkaAgentConfig, String consumerGroupName) {
    return new KafkaConsumer<>(toMap(getConsumerProperties(kafkaAgentConfig, consumerGroupName)));
  }

  private Properties getProducerProperties(KafkaAgentConfig kafkaAgentConfig) {
    final Properties props = getCommonProperties(kafkaAgentConfig);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, BytesSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, BytesSerializer.class.getName());
    return props;
  }

  public static KafkaProducer<Bytes, Bytes> createProducer(KafkaAgentConfig kafkaAgentConfig) {
    return new KafkaProducer<>(toMap(getProducerProperties(kafkaAgentConfig)));
  }
}
