package io.spoud.agoora.agents.kafka.config;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.spoud.agoora.agents.kafka.container.SchemaRegistryContainer;

import java.util.Map;

public class SchemaRegistryResource implements QuarkusTestResourceLifecycleManager {

  public static SchemaRegistryContainer schemaRegistry =
      new SchemaRegistryContainer("7.4.1")
          .withKafka(KafkaResource.kafka)
          .withNetwork(NetworkConfig.NETWORK);

  @Override
  public Map<String, String> start() {
    schemaRegistry.start();
    return Map.of("agoora.registry.confluent.url", schemaRegistry.getSchemaRegistryUrl());
  }

  @Override
  public void stop() {
    schemaRegistry.stop();
  }

  @Override
  public int order() {
    return 2;
  }
}
