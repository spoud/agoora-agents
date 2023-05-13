package io.spoud.agoora.agents.mqtt.config.data;

import io.smallrye.config.ConfigMapping;
import io.spoud.agoora.agents.api.config.AgooraAgentConfig;


@ConfigMapping(prefix = "agoora")
public interface MqttAgooraConfig extends AgooraAgentConfig {
  ScrapperConfig scrapper();
  MqttConfig mqtt();
}
