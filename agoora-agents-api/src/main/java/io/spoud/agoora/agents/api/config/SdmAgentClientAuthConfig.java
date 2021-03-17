package io.spoud.agoora.agents.api.config;

public interface SdmAgentClientAuthConfig {

  SdmAgentUserConfig getUser();

  String getServerUrl();

  String getRealm();

  default String getTrustStoreLocation() {
    return null;
  }

  default String getTrustStorePassword() {
    return null;
  }

  default boolean isIgnoreSsl() {
    return false;
  }
}
