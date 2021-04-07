package io.spoud.agoora.agents.kafka.service;

import io.quarkus.runtime.StartupEvent;
import io.spoud.agoora.agents.kafka.Constants;
import io.spoud.agoora.agents.kafka.data.KafkaConsumerGroup;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@ApplicationScoped
public class PropertyTemplateService {

  public static final String AGOORA_PROPERTY_TEMPLATES_KAFKA_TOPIC =
      "agoora.property-templates.kafka-topic";
  public static final String AGOORA_PROPERTY_TEMPLATES_KAFKA_CONSUMER_GROUP =
      "agoora.property-templates.kafka-consumer-group";

  @Getter private Map<String, String> kafkaTopicProperties = new HashMap<>();
  @Getter private Map<String, String> kafkaConsumerGroupProperties = new HashMap<>();

  public static Map<String, String> getMapFromConfig(String prefix) {
    final Config config = ConfigProvider.getConfig();
    final Iterable<String> propertyNames = config.getPropertyNames();
    return StreamSupport.stream(propertyNames.spliterator(), false)
        .filter(name -> name.startsWith(prefix) && !name.equalsIgnoreCase(prefix))
        .collect(
            Collectors.toMap(
                propertyName -> cleanupPropertyName(propertyName.substring(prefix.length() + 1)),
                propertyName -> config.getOptionalValue(propertyName, String.class).orElse("")));
  }

  /** Remove start and end double quotes */
  public static String cleanupPropertyName(String name) {
    if (name.startsWith("\"") && name.endsWith("\"")) {
      return name.substring(1, name.length() - 1);
    }
    return name;
  }

  void onStartup(@Observes StartupEvent event) {
    kafkaTopicProperties = getMapFromConfig(AGOORA_PROPERTY_TEMPLATES_KAFKA_TOPIC);
    kafkaConsumerGroupProperties = getMapFromConfig(AGOORA_PROPERTY_TEMPLATES_KAFKA_CONSUMER_GROUP);

    LOG.info("Properties: {} \n {}", kafkaTopicProperties, kafkaConsumerGroupProperties);
  }

  public Map<String, String> mapExternalPropertiesForTopic(KafkaTopic kafkaTopic) {
    Map<String, String> properties = new HashMap<>();
    kafkaTopicProperties.forEach(
        (k, v) -> {
          String value =
              v.replaceAll(
                      Pattern.quote(Constants.SDM_TEMPLATE_KAFKA_TOPIC), kafkaTopic.getTopicName())
                  .replaceAll(
                      Pattern.quote(Constants.SDM_TEMPLATE_RESOURCE_ID),
                      kafkaTopic.getDataPortId() != null ? kafkaTopic.getDataPortId() : "");
          properties.put(Constants.AGOORA_EXTERNAL_PROPERTIES_PREFIX + k, value);
        });
    return properties;
  }

  public Map<String, String> mapExternalPropertiesForConsumerGroup(
      KafkaConsumerGroup kafkaConsumerGroup) {
    var properties = new HashMap<String, String>();
    kafkaConsumerGroupProperties.forEach(
        (k, v) -> {
          String value =
              v.replaceAll(
                      Pattern.quote(Constants.SDM_TEMPLATE_KAFKA_TOPIC),
                      kafkaConsumerGroup.getTopicName())
                  .replaceAll(
                      Pattern.quote(Constants.SDM_TEMPLATE_KAFKA_CONSUMER_GROUP),
                      kafkaConsumerGroup.getConsumerGroupName())
                  .replaceAll(
                      Pattern.quote(Constants.SDM_TEMPLATE_RESOURCE_ID),
                      kafkaConsumerGroup.getDataSubscriptionStateId() != null
                          ? kafkaConsumerGroup.getDataSubscriptionStateId()
                          : "");
          properties.put(Constants.AGOORA_EXTERNAL_PROPERTIES_PREFIX + k, value);
        });
    return properties;
  }
}
