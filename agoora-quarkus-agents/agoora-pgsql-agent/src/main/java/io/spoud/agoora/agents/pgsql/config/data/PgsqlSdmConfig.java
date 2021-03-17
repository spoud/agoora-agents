package io.spoud.agoora.agents.pgsql.config.data;

import io.quarkus.arc.config.ConfigProperties;
import io.spoud.agoora.agents.api.config.SdmAgentConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@ConfigProperties(prefix = "sdm")
@NoArgsConstructor
public class PgsqlSdmConfig implements SdmAgentConfig {
  private SdmScrapperConfig scrapper;
  private SdmClientAuthConfig auth;
  private SdmEndpointConfig logistics;
  private SdmEndpointConfig hooks;
  private SdmEndpointConfig schema;
  private SdmEndpointConfig looker;
  private SdmEndpointConfig blob;
  private SdmEndpointConfig profiler;
  private SdmTransportConfig transport;
}
