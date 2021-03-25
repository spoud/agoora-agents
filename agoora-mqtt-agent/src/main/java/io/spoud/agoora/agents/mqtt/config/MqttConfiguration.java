package io.spoud.agoora.agents.mqtt.config;

import io.spoud.agoora.agents.mqtt.config.data.MqttSdmConfig;
import io.spoud.agoora.agents.mqtt.config.data.SdmMqttConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

@Slf4j
@Dependent
public class MqttConfiguration {

  @Produces
  @Singleton
  MqttClient mqttClient(MqttSdmConfig config) {
    final SdmMqttConfig mqtt = config.getMqtt();
    try {
      final MqttClient client =
          new MqttClient(mqtt.getBroker(), mqtt.getClientId(), new MemoryPersistence());
      MqttConnectOptions options = new MqttConnectOptions();

      mqtt.getUsername()
          .filter(StringUtils::isNotBlank)
          .map(String::trim)
          .ifPresent(options::setUserName);
      mqtt.getPassword()
          .filter(StringUtils::isNotBlank)
          .map(String::trim)
          .map(String::toCharArray)
          .ifPresent(options::setPassword);

      options.setCleanSession(true);
      options.setKeepAliveInterval(40);
      options.setAutomaticReconnect(true);
      options.setConnectionTimeout(10);
      options.setMaxInflight(100);

      LOG.debug("Connecting to broker: {}", mqtt);
      client.connect(options);
      LOG.info("Connected to {}", mqtt.getBroker());
      return client;
    } catch (MqttException ex) {
      LOG.error("Error while connecting to {}", mqtt.getBroker(), ex);
      System.exit(1);
    }
    return null;
  }
}
