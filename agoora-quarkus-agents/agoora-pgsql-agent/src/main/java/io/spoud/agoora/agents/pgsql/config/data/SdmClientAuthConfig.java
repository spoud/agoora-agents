package io.spoud.agoora.agents.pgsql.config.data;

import io.spoud.agoora.agents.api.config.SdmAgentClientAuthConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SdmClientAuthConfig implements SdmAgentClientAuthConfig {

  private SdmUserConfig user;

  private String serverUrl;

  private String realm;
}
