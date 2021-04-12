package io.spoud.agoora.agents.kafka.config.data;

import io.quarkus.arc.config.ConfigProperties;
import io.spoud.agoora.agents.api.config.AgooraAgentConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ConfigProperties(prefix = "agoora")
@ToString(callSuper = true)
public class KafkaAgentConfig extends AgooraAgentConfig {
  private ScrapperConfig scrapper;
  private KafkaConfig kafka;
  private RegistryConfig registry;
  private PropertyTemplatesConfig propertyTemplates = new PropertyTemplatesConfig();
}
