package io.spoud.agoora.agents.mqtt.config;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.spoud.agoora.agents.mqtt.container.MqttContainer;

import java.util.Map;

public class MqttResource implements QuarkusTestResourceLifecycleManager {

  private MqttContainer mqtt = new MqttContainer();

  @Override
  public Map<String, String> start() {
    mqtt.start();
    return Map.of("sdm.mqtt.broker", mqtt.getBroker());
  }

  @Override
  public void stop() {
    mqtt.stop();
  }
}
