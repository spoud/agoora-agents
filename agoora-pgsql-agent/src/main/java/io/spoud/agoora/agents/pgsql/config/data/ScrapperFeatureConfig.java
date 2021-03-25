package io.spoud.agoora.agents.pgsql.config.data;

import lombok.Data;

import java.time.Duration;

@Data
public class ScrapperFeatureConfig {
  private boolean enabled;
  private Duration initialDelay;
  private Duration interval = Duration.ofMinutes(15);
}
