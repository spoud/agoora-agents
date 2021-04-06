package io.spoud.agoora.agents.kafka.config.data;

import lombok.Data;
import lombok.ToString;

import java.util.Optional;

@Data
@ToString(exclude = {"loginSecret", "keyStorePassword", "trustStorePassword" })
public class KafkaConfig {

  private String topicFilterRegex;
  private String consumerGroupFilterRegex;
  private String bootstrapServers;
  private String protocol;
  private Optional<String> loginKey;
  private Optional<String> loginSecret;
  private Optional<String> keyStoreLocation;
  private Optional<String> keyStorePassword;
  private Optional<String> trustStoreLocation;
  private Optional<String> trustStorePassword;
}
