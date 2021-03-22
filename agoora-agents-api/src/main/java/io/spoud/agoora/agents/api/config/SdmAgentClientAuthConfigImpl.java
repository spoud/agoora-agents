package io.spoud.agoora.agents.api.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SdmAgentClientAuthConfigImpl implements SdmAgentClientAuthConfig {

  private SdmAgentUserConfig user;

  private String serverUrl;

  private String realm;

}
