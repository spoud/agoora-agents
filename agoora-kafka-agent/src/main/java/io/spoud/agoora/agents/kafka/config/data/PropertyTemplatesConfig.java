package io.spoud.agoora.agents.kafka.config.data;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Data
public class PropertyTemplatesConfig {

  private Optional<String> kafkaTopic;
  private Optional<String> kafkaConsumerGroup;
}
