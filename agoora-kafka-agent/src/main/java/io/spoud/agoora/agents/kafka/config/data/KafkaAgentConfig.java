package io.spoud.agoora.agents.kafka.config.data;

import io.smallrye.config.ConfigMapping;
import io.spoud.agoora.agents.api.config.AgooraAgentConfig;

@ConfigMapping(prefix = "agoora")
public interface KafkaAgentConfig extends AgooraAgentConfig {
  ScrapperConfig scrapper();
  KafkaConfig kafka();
  RegistryConfig registry();
  PropertyTemplatesConfig propertyTemplates();

  SchemaCacheConfig schemaCache();
}
