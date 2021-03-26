package io.spoud.agoora.agents.kafka.schema.confluent;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.sdm.schema.domain.v1alpha.Schema;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class ConfluentSchemaRegistryTest {

  @Inject @RestClient ConfluentRegistryResource confluentRegistryResource;
  @Inject KafkaAgentConfig kafkaAgentConfig;

  ConfluentSchemaRegistry confluentSchemaRegistry;

  @BeforeEach
  void setup() {
    confluentSchemaRegistry =
        new ConfluentSchemaRegistry(confluentRegistryResource, kafkaAgentConfig.getRegistry().getConfluent());
  }

  @Test
  void testSchema() {
    final List<Schema> schemas = confluentSchemaRegistry.getNewSchemaForTopic("gc-to-remove");
    assertThat(schemas).isNotEmpty();
  }

  @Test
  void testDeepDiveTool() {
    assertThat(confluentSchemaRegistry.getDeepDiveToolUrl("my-topic"))
        .isPresent()
        .hasValue("https://my-url/my-topic/ui");
  }
}
