package io.spoud.agoora.agents.openapi.config.data;

import lombok.Data;

@Data
public class SdmScrapperConfig {
  private Long samplesSize;
  private SdmScrapperFeatureConfig state;
  private SdmScrapperFeatureConfig hooks;
}
