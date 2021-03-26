package io.spoud.agoora.agents.kafka.config.data;

import io.quarkus.arc.config.ConfigProperties;
import io.spoud.agoora.agents.api.config.AgooraAgentConfig;
import lombok.Data;

@Data
@ConfigProperties(prefix = "agoora")
public class KafkaAgentConfig extends AgooraAgentConfig {
  private ScrapperConfig scrapper;
  private KafkaConfig kafka;
  private RegistryConfig registry;
  private PropertyTemplatesConfig propertyTemplates = new PropertyTemplatesConfig();
}
