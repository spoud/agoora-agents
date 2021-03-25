package io.spoud.agoora.agents.pgsql.config.data;

import lombok.Data;

@Data
public class ScrapperConfig {
  private Integer samplesSize;
  private ScrapperFeatureConfig state;
  private ScrapperFeatureConfig profiling;
  private ScrapperFeatureConfig hooks;
}
