package io.spoud.agoora.agents.mqtt.config;

import io.spoud.agoora.agents.mqtt.config.data.MqttAgooraConfig;
import io.spoud.agoora.agents.mqtt.config.data.MqttConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Slf4j
@Dependent
public class MqttConfiguration {

  @Produces
  @Singleton
  MqttClient mqttClient(MqttAgooraConfig config) {
    final MqttConfig mqtt = config.mqtt();
    try {

      final String clientId = mqtt.clientId() + "-" + MqttClient.generateClientId();

      final MqttClient client = new MqttClient(mqtt.broker(), clientId, new MemoryPersistence());
      MqttConnectOptions options = new MqttConnectOptions();

      mqtt.username()
          .filter(StringUtils::isNotBlank)
          .map(String::trim)
          .ifPresent(options::setUserName);
      mqtt.password()
          .filter(StringUtils::isNotBlank)
          .map(String::trim)
          .map(String::toCharArray)
          .ifPresent(options::setPassword);

      options.setCleanSession(true);
      options.setAutomaticReconnect(true);
      options.setMaxInflight(1000);


      client.setCallback(
          new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
              LOG.error("MQTT connection lost", cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
              // ignore
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
              // ignore
            }
          });

      LOG.debug("Connecting to broker: {}", mqtt);
      client.connect(options);
      LOG.info("Connected to {}", mqtt.broker());
      return client;
    } catch (MqttException ex) {
      LOG.error("Error while connecting to {}", mqtt.broker(), ex);
      System.exit(1);
    }
    return null;
  }
}
