package io.spoud.agoora.agents.openapi.config.data;

import lombok.Data;

public interface ScrapperConfig {
  Long samplesSize();
  ScrapperFeatureConfig state();
  ScrapperFeatureConfig hooks();
}
