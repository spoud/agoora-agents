package io.spoud.agoora.agents.api.config;

public interface SdmAgentEndpointConfig {
  String getEndpoint();

  default boolean isInsecure() {
    return false;
  }

}
