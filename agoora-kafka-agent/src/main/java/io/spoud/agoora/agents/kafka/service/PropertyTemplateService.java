package io.spoud.agoora.agents.kafka.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spoud.agoora.agents.kafka.Constants;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.agoora.agents.kafka.data.KafkaConsumerGroup;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class PropertyTemplateService {
  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final KafkaAgentConfig config;
  private final AtomicReference<Map<String, String>> kafkaTopicMapCache =
      new AtomicReference<>(null);
  private final AtomicReference<Map<String, String>> kafkaConsumerGroupCache =
      new AtomicReference<>(null);

  public Map<String, String> getKafkaTopicMap() {
    return getMap(kafkaTopicMapCache, config.propertyTemplates().kafkaTopic());
  }

  public Map<String, String> getKafkaConsumerGroupMap() {
    return getMap(kafkaConsumerGroupCache, config.propertyTemplates().kafkaConsumerGroup());
  }

  Map<String, String> getMap(AtomicReference<Map<String, String>> cache, Optional<String> opt) {
    return cache.updateAndGet(
        old -> {
          if (old == null) {
            return opt.map(
                    str -> {
                      TypeReference<Map<String, String>> typeRef = new TypeReference<>() {};
                      try {
                        return OBJECT_MAPPER.readValue(str, typeRef);
                      } catch (JsonProcessingException ex) {
                        LOG.error("unable to parse json for '{}'", str, ex);
                      }
                      return null;
                    })
                .orElse(Collections.emptyMap());
          } else {
            return old;
          }
        });
  }

  public Map<String, String> mapExternalPropertiesForTopic(KafkaTopic kafkaTopic) {
    Map<String, String> properties = new HashMap<>();
    getKafkaTopicMap()
        .forEach(
            (k, v) -> {
              String value =
                  v.replaceAll(
                          Pattern.quote(Constants.SDM_TEMPLATE_KAFKA_TOPIC),
                          kafkaTopic.getTopicName())
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
    getKafkaConsumerGroupMap()
        .forEach(
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
