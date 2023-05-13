package io.spoud.agoora.agents.kafka.config.data;

import java.util.Optional;

public interface PropertyTemplatesConfig {

  Optional<String> kafkaTopic();
  Optional<String> kafkaConsumerGroup();
}
