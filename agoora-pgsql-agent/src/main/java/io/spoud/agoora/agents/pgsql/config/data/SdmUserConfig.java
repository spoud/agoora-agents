package io.spoud.agoora.agents.pgsql.config.data;

import io.spoud.agoora.agents.api.config.SdmAgentUserConfig;
import lombok.Data;

@Data
public class SdmUserConfig implements SdmAgentUserConfig {

  private String name;

  private String token;
}
