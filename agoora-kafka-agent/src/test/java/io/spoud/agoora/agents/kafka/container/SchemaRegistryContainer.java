package io.spoud.agoora.agents.kafka.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;

public class SchemaRegistryContainer extends GenericContainer<SchemaRegistryContainer> {

  public SchemaRegistryContainer() {
    super("confluentinc/cp-schema-registry");
    withExposedPorts(8081);
  }

  public SchemaRegistryContainer withKafka(KafkaContainer kafka) {
    withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry");
    withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081");
    withEnv(
        "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
        "PLAINTEXT://" + kafka.getNetworkAliases().get(0) + ":9092");
    return self();
  }

  public String getSchemaRegistryUrl() {
    return "http://" + getContainerIpAddress() + ":" + getMappedPort(8081);
  }
}
