package io.spoud.agoora.agents.api.config;

public interface AgooraAgentConfig {
  AgooraAgentClientAuthConfig auth();

  AgooraTransportConfig transport();

  AgooraAgentEndpointConfig logistics();

  AgooraAgentEndpointConfig hooks();

  AgooraAgentEndpointConfig schema();

  AgooraAgentEndpointConfig looker();

  AgooraAgentEndpointConfig blob();

  AgooraAgentEndpointConfig profiler();
}
