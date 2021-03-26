package io.spoud.agoora.agents.mqtt.config;

import io.spoud.agoora.agents.mqtt.config.data.MqttAgooraConfig;
import io.spoud.agoora.agents.mqtt.config.data.MqttConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

@Slf4j
@Dependent
public class MqttConfiguration {

  @Produces
  @Singleton
  MqttClient mqttClient(MqttAgooraConfig config) {
    final MqttConfig mqtt = config.getMqtt();
    try {

      final String clientId = mqtt.getClientId() + "-" + MqttClient.generateClientId();

      final MqttClient client = new MqttClient(mqtt.getBroker(), clientId, new MemoryPersistence());
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
      LOG.info("Connected to {}", mqtt.getBroker());
      return client;
    } catch (MqttException ex) {
      LOG.error("Error while connecting to {}", mqtt.getBroker(), ex);
      System.exit(1);
    }
    return null;
  }
}
