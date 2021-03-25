package io.spoud.agoora.agents.mqtt.config.data;

import io.quarkus.arc.config.ConfigProperties;
import io.spoud.agoora.agents.api.config.AgooraAgentConfig;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@ConfigProperties(prefix = "agoora")
@NoArgsConstructor
public class MqttAgooraConfig extends AgooraAgentConfig {
  private ScrapperConfig scrapper;
  private MqttConfig mqtt;
}
