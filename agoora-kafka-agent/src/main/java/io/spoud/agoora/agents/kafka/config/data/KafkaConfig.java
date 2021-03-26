package io.spoud.agoora.agents.kafka.config.data;

import lombok.Data;

import java.util.Optional;

@Data
public class KafkaConfig {

  private String topicFilterRegex;
  private String consumerGroupFilterRegex;
  private String bootstrapServers;
  private String protocol;
  private Optional<String> keyStoreLocation;
  private Optional<String> keyStorePassword;
  private Optional<String> trustStoreLocation;
  private Optional<String> trustStorePassword;
}
