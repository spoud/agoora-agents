package io.spoud.agoora.agents.mqtt.container;

import org.testcontainers.containers.GenericContainer;

public class MqttContainer extends GenericContainer<MqttContainer> {

  public static final int MQTT_PORT = 1883;

  public MqttContainer() {
    super("eclipse-mosquitto");
    withCommand("mosquitto -c /mosquitto-no-auth.conf");
    withExposedPorts(MQTT_PORT);
  }

  public int getMqttPort() {
    return getMappedPort(MQTT_PORT);
  }

  public String getBroker() {
    return "tcp://" + getContainerIpAddress() + ":" + getMqttPort();
  }
}
