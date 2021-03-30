package io.spoud.agoora.agents.kafka.schema;

import io.spoud.sdm.schema.domain.v1alpha.Schema;

import java.util.Optional;

public interface SchemaRegistryClient {
  Optional<Schema> getLatestSchemaForTopic(String topic, KafkaStreamPart part);

  Optional<String> getDeepDiveToolUrl(String topic);
}
