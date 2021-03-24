package io.spoud.agoora.agents.api.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// TODO should be optional !
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SdmAgentConfig {
  private SdmAgentClientAuthConfig auth;

  @Builder.Default private SdmAgentEndpointConfig logistics = null;

  @Builder.Default private SdmAgentEndpointConfig hooks = null;

  @Builder.Default private SdmAgentEndpointConfig schema = null;

  @Builder.Default private SdmAgentEndpointConfig looker = null;

  @Builder.Default private SdmAgentEndpointConfig blob = null;

  @Builder.Default private SdmAgentEndpointConfig profiler = null;
}
