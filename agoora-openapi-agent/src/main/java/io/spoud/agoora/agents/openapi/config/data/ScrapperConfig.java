package io.spoud.agoora.agents.openapi.config.data;

import lombok.Data;

@Data
public class ScrapperConfig {
  private Long samplesSize;
  private ScrapperFeatureConfig state;
  private ScrapperFeatureConfig hooks;
}
