package io.spoud.agoora.agents.openapi.config.data;

import io.quarkus.arc.config.ConfigProperties;
import io.spoud.agoora.agents.api.config.SdmAgentConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@ConfigProperties(prefix = "sdm")
@NoArgsConstructor
public class OpenApiSdmConfig extends SdmAgentConfig {
  private SdmScrapperConfig scrapper;
  private OpenApiConfig openapi;
}
