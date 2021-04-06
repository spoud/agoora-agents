package io.spoud.agoora.agents.kafka.config.data;

import lombok.Data;
import lombok.ToString;

import java.util.Optional;

@Data
@ToString(exclude = {"secret", "keyStorePassword", "trustStorePassword" })
public class KafkaConfig {

  private String topicFilterRegex;
  private String consumerGroupFilterRegex;
  private String bootstrapServers;
  private String protocol;
  private Optional<String> key;
  private Optional<String> secret;
  private Optional<String> keyStoreLocation;
  private Optional<String> keyStorePassword;
  private Optional<String> trustStoreLocation;
  private Optional<String> trustStorePassword;
}
