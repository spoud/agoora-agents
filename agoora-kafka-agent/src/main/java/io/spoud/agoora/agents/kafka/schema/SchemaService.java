package io.spoud.agoora.agents.kafka.schema;

import io.grpc.StatusRuntimeException;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.schema.domain.v1alpha.Schema;
import io.spoud.sdm.schema.domain.v1alpha.SchemaSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
//@ApplicationScoped
@RequiredArgsConstructor
public class SchemaService {

  public static final String PROPETIES_DEEP_DIVE_TOOL_SCHEMA_REGISTRY =
      "sdm.transport.external.schema-registry.url";

  private final KafkaAgentConfig kafkaAgentConfig;
  private final SchemaClient schemaClient;
  private final List<SchemaRegistryClient> schemaRegistries;

  public Map<String, String> update(String topicName, String dataPortId) {
    Map<String, String> properties = new HashMap<>();

    for (SchemaRegistryClient schemaRegistry : schemaRegistries) {
      List<Schema> schemaForTopic = schemaRegistry.getNewSchemaForTopic(topicName);
      LOG.debug(
          "{} schema(s) found for the topic {} in the registry {}",
          schemaForTopic.size(),
          topicName,
          schemaRegistry);

      for (Schema schema : schemaForTopic) {
        try {
          Schema saved =
              schemaClient.saveSchema(
                  ResourceEntity.Type.DATA_PORT,
                  dataPortId,
                  kafkaAgentConfig.getTransport().getAgooraPathObject().getResourceGroupPath(),
                  schema.getContent(),
                  SchemaSource.Type.REGISTRY,
                  schema.getEncoding());
          LOG.info("Successfully saved schema with uuid '{}'", saved.getId());
        } catch (StatusRuntimeException ex) {
          LOG.error("Unable to save schema '{}'", schema, ex);
        }
      }

      if (!schemaForTopic.isEmpty()) {
        schemaRegistry
            .getDeepDiveToolUrl(topicName)
            .ifPresent(url -> properties.put(PROPETIES_DEEP_DIVE_TOOL_SCHEMA_REGISTRY, url));
      }
    }
    return properties;
  }
}
