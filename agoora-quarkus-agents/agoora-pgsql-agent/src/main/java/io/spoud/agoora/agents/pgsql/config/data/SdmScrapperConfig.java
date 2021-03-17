package io.spoud.agoora.agents.pgsql.config.data;

import lombok.Data;

@Data
public class SdmScrapperConfig {
  private Long samplesSize;
  private SdmScrapperFeatureConfig state;
  private SdmScrapperFeatureConfig profiling;
  private SdmScrapperFeatureConfig hooks;
}
