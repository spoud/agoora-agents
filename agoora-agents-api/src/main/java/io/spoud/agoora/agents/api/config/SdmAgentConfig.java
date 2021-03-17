package io.spoud.agoora.agents.api.config;

public interface SdmAgentConfig {
  SdmAgentClientAuthConfig getAuth();

  default SdmAgentEndpointConfig getLogistics() {
    return null;
  }

  default SdmAgentEndpointConfig getHooks() {
    return null;
  }

  default SdmAgentEndpointConfig getSchema() {
    return null;
  }

  default SdmAgentEndpointConfig getLooker() {
    return null;
  }

  default SdmAgentEndpointConfig getBlob() {
    return null;
  }

  default SdmAgentEndpointConfig getProfiler() {
    return null;
  }
}
