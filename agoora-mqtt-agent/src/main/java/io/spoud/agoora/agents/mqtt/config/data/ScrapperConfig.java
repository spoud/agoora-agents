package io.spoud.agoora.agents.mqtt.config.data;

import lombok.Data;

import java.time.Duration;

@Data
public class ScrapperConfig {
  private Integer maxSamples;
  private Duration period;
  private Duration maxWait;
  private ScrapperFeatureConfig profiling;
  private ScrapperFeatureConfig hooks;
}
