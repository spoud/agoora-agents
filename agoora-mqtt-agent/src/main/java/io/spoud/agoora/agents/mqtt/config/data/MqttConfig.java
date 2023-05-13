package io.spoud.agoora.agents.mqtt.config.data;

import java.util.Optional;

public interface MqttConfig {
  String broker();
  String clientId();
  String paths();
  Optional<String> username();
  Optional<String> password();
}
