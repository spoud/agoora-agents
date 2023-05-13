package io.spoud.agoora.agents.api.config;

public interface AgooraAgentClientAuthConfig {

  AgooraAgentUserConfig user();

  String serverUrl();

  String realm();

  default String trustStoreLocation(){return null;}

  default String trustStorePassword() {return null;}

  default boolean ignoreSsl(){return false;}
}
