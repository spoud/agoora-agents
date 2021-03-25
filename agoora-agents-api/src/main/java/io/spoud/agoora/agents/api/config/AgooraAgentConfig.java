package io.spoud.agoora.agents.api.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgooraAgentConfig {
  private AgooraAgentClientAuthConfig auth;

  private AgooraTransportConfig transport;

  @Builder.Default private AgooraAgentEndpointConfig logistics = null;

  @Builder.Default private AgooraAgentEndpointConfig hooks = null;

  @Builder.Default private AgooraAgentEndpointConfig schema = null;

  @Builder.Default private AgooraAgentEndpointConfig looker = null;

  @Builder.Default private AgooraAgentEndpointConfig blob = null;

  @Builder.Default private AgooraAgentEndpointConfig profiler = null;
}
