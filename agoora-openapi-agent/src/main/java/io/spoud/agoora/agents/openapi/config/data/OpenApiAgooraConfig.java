package io.spoud.agoora.agents.openapi.config.data;

import io.smallrye.config.ConfigMapping;
import io.spoud.agoora.agents.api.config.AgooraAgentConfig;

@ConfigMapping(prefix = "agoora")
public interface OpenApiAgooraConfig extends AgooraAgentConfig {
  ScrapperConfig scrapper();
  OpenApiConfig openapi();
}
