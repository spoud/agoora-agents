package io.spoud.agoora.agents.api.config;

import io.smallrye.config.WithDefault;

public interface AgooraAgentConfig {
  AgooraAgentClientAuthConfig auth();

  AgooraTransportConfig transport();

  AgooraAgentEndpointConfig logistics();

  AgooraAgentEndpointConfig hooks();

  AgooraAgentEndpointConfig schema();

  AgooraAgentEndpointConfig looker();

  AgooraAgentEndpointConfig profiler();

  // Self-reported build version of this agent, included in the periodic operational
  // metrics heartbeat. Injected at deploy time via AGOORA_AGENT_VERSION.
  @WithDefault("default")
  String agentVersion();
}
