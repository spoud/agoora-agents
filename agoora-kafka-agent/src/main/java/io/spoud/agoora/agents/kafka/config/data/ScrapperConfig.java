package io.spoud.agoora.agents.kafka.config.data;

import lombok.Data;

import java.time.Duration;

@Data
public class ScrapperConfig {
  private Integer maxSamples;
  private Duration period;
  private ScrapperFeatureConfig profiling;
  private ScrapperFeatureConfig hooks;
}
