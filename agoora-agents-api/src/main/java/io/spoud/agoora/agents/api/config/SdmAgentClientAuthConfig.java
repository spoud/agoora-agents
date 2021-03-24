package io.spoud.agoora.agents.api.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SdmAgentClientAuthConfig {

  private SdmAgentUserConfig user;

  private String serverUrl;

  private String realm;

  @Builder.Default private String trustStoreLocation = null;

  @Builder.Default private String trustStorePassword = null;

  @Builder.Default private boolean ignoreSsl = false;
}
