package io.spoud.agoora.agents.mqtt.config.data;

import lombok.Data;
import lombok.ToString;

import java.time.Duration;
import java.util.Optional;

@Data
@ToString(exclude = {"password"})
public class MqttConfig {
  private String broker;
  private String clientId;
  private String paths;
  private Optional<String> username;
  private Optional<String> password;
}
