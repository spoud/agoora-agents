package io.spoud.agoora.agents.kafka.config.data;

import java.util.Optional;


public interface KafkaConfig {

  String topicFilterRegex();
  String consumerGroupFilterRegex();
  String bootstrapServers();
  String protocol();
  Optional<String> key();
  Optional<String> secret();
  Optional<String> keyStoreLocation();
  Optional<String> keyStorePassword();
  Optional<String> trustStoreLocation();
  Optional<String> trustStorePassword();
}
