package io.spoud.agoora.agents.pgsql.config.data;

import lombok.Data;

import java.time.Duration;

@Data
public class ScrapperConfig {
  private Integer samplesSize;

  private Duration initialDelay;
  private Duration interval = Duration.ofMinutes(15);

  private ScrapperFeatureConfig state;
  private ScrapperFeatureConfig profiling;
  private ScrapperFeatureConfig hooks;
}
