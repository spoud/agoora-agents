package io.spoud.agoora.agents.kafka.schema;

import io.grpc.StatusRuntimeException;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.kafka.Constants;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.schema.domain.v1alpha.Schema;
import io.spoud.sdm.schema.domain.v1alpha.SchemaEncoding.Type;
import io.spoud.sdm.schema.domain.v1alpha.SchemaSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class SchemaService {

  private final KafkaAgentConfig kafkaAgentConfig;
  private final SchemaClient schemaClient;
  private final Instance<SchemaRegistryClient> schemaRegistries;

  public Map<String, String> update(String topicName, String dataPortId) {
    Map<String, String> properties = new HashMap<>();
    try {
      for (SchemaRegistryClient schemaRegistry : schemaRegistries) {

        Schema valueSchema = schemaRegistry
            .getLatestSchemaForTopic(topicName, KafkaStreamPart.VALUE)
            .orElseGet(
                () -> {
                  LOG.debug(
                      "No {} schema found for topic {} in the registry {}", KafkaStreamPart.VALUE, topicName,
                      schemaRegistry.getDeepDiveToolUrl(topicName).orElse("NOT_FOUND"));
                  return null;
                });

        Schema keySchema = schemaRegistry
            .getLatestSchemaForTopic(topicName, KafkaStreamPart.KEY)
            .orElseGet(
                () -> {
                  LOG.debug(
                      "No {} schema found for topic {} in the registry {}", KafkaStreamPart.KEY, topicName,
                      schemaRegistry.getDeepDiveToolUrl(topicName).orElse("NOT_FOUND"));
                  return null;
                });

        saveSchema(topicName, dataPortId, properties, schemaRegistry, valueSchema, keySchema);


      }
    } catch (Exception ex) {
      LOG.error("Unable to update schema", ex);
    }
    return properties;
  }

  private void saveSchema(String topicName, String dataPortId, Map<String, String> properties, SchemaRegistryClient schemaRegistry, Schema valueSchema, Schema keySchema) {
    Type valueSchemaEncoding = Type.UNKNOWN;
    String valueSchemaContent = "";
    Type keySchemaEncoding = Type.UNKNOWN;
    String keySchemaContent = "";

    if (valueSchema == null && keySchema == null) {
      return;
    }

    if (valueSchema != null) {
      valueSchemaContent = valueSchema.getContent();
      valueSchemaEncoding = valueSchema.getEncoding();
    }

    if (keySchema != null) {
      keySchemaContent = keySchema.getContent();
      keySchemaEncoding = keySchema.getEncoding();
    }

    try {
      LOG.debug(
          "One KEY/VALUE schema found for the topic {} in the registry {}",
          topicName,
          schemaRegistry.getDeepDiveToolUrl(topicName));

      Schema saved =
          schemaClient.saveSchema(
              ResourceEntity.Type.DATA_PORT,
              dataPortId,
              kafkaAgentConfig
                  .getTransport()
                  .getAgooraPathObject()
                  .getResourceGroupPath(),
              valueSchemaContent,
              SchemaSource.Type.REGISTRY,
              valueSchemaEncoding,
              keySchemaContent,
              keySchemaEncoding);

      LOG.info("Successfully saved schema for topic {} with uuid '{}'", topicName,
          saved.getId());

      schemaRegistry
          .getDeepDiveToolUrl(topicName)
          .ifPresent(
              url ->
                  properties.put(
                      Constants.PROPETIES_DEEP_DIVE_TOOL_SCHEMA_REGISTRY, url));
    } catch (StatusRuntimeException ex) {
      LOG.error("Unable to save value schema '{}' and key schema {} for topic {}", valueSchema,
          keySchema, topicName, ex);
    }
  }
}
