package io.spoud.agoora.agents.pgsql.config.data;


import io.spoud.agoora.agents.api.config.SdmAgentEndpointConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SdmEndpointConfig implements SdmAgentEndpointConfig {
  private String endpoint;
  private String cluster = "default";
  private boolean insecure = false;
}
