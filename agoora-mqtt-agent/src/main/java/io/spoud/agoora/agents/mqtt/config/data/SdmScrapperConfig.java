package io.spoud.agoora.agents.mqtt.config.data;

import lombok.Data;

import java.time.Duration;

@Data
public class SdmScrapperConfig {
  private Integer maxSamples;
  private Duration period;
  private Duration maxWait;
  private SdmScrapperFeatureConfig profiling;
  private SdmScrapperFeatureConfig hooks;
}
