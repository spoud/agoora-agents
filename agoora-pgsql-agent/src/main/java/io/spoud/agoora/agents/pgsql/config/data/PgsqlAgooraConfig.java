package io.spoud.agoora.agents.pgsql.config.data;

import io.smallrye.config.ConfigMapping;
import io.spoud.agoora.agents.api.config.AgooraAgentConfig;

@ConfigMapping(prefix = "agoora")
public interface PgsqlAgooraConfig extends AgooraAgentConfig {
  ScrapperConfig scrapper();
}
