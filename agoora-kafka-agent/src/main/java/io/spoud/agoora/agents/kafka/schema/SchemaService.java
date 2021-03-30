package io.spoud.agoora.agents.kafka.schema;

import io.grpc.StatusRuntimeException;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.kafka.Constants;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.schema.domain.v1alpha.Schema;
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

    for (SchemaRegistryClient schemaRegistry : schemaRegistries) {
      schemaRegistry
          .getLatestSchemaForTopic(topicName, KafkaStreamPart.VALUE)
          .ifPresentOrElse(
              schema -> {
                LOG.debug(
                    "One schema found for the topic {} in the registry {}",
                    topicName,
                    schemaRegistry);

                try {
                  Schema saved =
                      schemaClient.saveSchema(
                          ResourceEntity.Type.DATA_PORT,
                          dataPortId,
                          kafkaAgentConfig
                              .getTransport()
                              .getAgooraPathObject()
                              .getResourceGroupPath(),
                          schema.getContent(),
                          SchemaSource.Type.REGISTRY,
                          schema.getEncoding());
                  LOG.info("Successfully saved schema with uuid '{}'", saved.getId());
                } catch (StatusRuntimeException ex) {
                  LOG.error("Unable to save schema '{}'", schema, ex);
                }

                schemaRegistry
                    .getDeepDiveToolUrl(topicName)
                    .ifPresent(
                        url ->
                            properties.put(
                                Constants.PROPETIES_DEEP_DIVE_TOOL_SCHEMA_REGISTRY, url));
              },
              () -> {
                LOG.debug(
                    "No schema found for topic {} in the registry {}", topicName, schemaRegistry);
              });
    }
    return properties;
  }
}
