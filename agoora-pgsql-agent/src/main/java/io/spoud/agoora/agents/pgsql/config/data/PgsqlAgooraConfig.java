package io.spoud.agoora.agents.pgsql.config.data;

import io.quarkus.arc.config.ConfigProperties;
import io.spoud.agoora.agents.api.config.AgooraAgentConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@ConfigProperties(prefix = "agoora")
@NoArgsConstructor
public class PgsqlAgooraConfig extends AgooraAgentConfig {
  private ScrapperConfig scrapper;
}
