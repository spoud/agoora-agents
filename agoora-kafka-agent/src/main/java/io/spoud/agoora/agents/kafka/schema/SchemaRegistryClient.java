package io.spoud.agoora.agents.kafka.schema;

import io.spoud.sdm.schema.domain.v1alpha.Schema;

import java.util.List;
import java.util.Optional;

public interface SchemaRegistryClient {
  List<Schema> getNewSchemaForTopic(String topic);
  Optional<String> getDeepDiveToolUrl(String topic);
}
