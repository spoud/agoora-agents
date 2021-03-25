package io.spoud.agoora.agents.openapi.config.data;

import lombok.Data;

import java.time.Duration;

@Data
public class SdmScrapperFeatureConfig {
  private boolean enabled;
  private Duration initialDelay;
  private Duration interval = Duration.ofMinutes(15);
}
