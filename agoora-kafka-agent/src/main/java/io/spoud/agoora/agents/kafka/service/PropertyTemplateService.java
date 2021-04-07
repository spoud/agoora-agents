package io.spoud.agoora.agents.kafka.service;

import io.quarkus.runtime.StartupEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@ApplicationScoped
public class PropertyTemplateService {

  public static final String AGOORA_PROPERTY_TEMPLATES_KAFKA_TOPIC =
      "agoora.property-templates.kafka-topic";
  public static final String AGOORA_PROPERTY_TEMPLATES_KAFKA_CONSUMER_GROUP =
      "agoora.property-templates.kafka-consumer-group";

  @Getter private Map<String, String> kafkaTopic = new HashMap<>();
  @Getter private Map<String, String> kafkaConsumerGroup = new HashMap<>();

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
    kafkaTopic = getMapFromConfig(AGOORA_PROPERTY_TEMPLATES_KAFKA_TOPIC);
    kafkaConsumerGroup = getMapFromConfig(AGOORA_PROPERTY_TEMPLATES_KAFKA_CONSUMER_GROUP);

    LOG.info("Properties: {} \n {}", kafkaTopic, kafkaConsumerGroup);
  }
}
