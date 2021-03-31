package io.spoud.agoora.agents.kafka.schema.confluent;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.kafka.AbstractService;
import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import io.spoud.agoora.agents.kafka.utils.SchemaRegistryUtil;
import io.spoud.sdm.schema.domain.v1alpha.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class ConfluentSchemaRegistryTest extends AbstractService {

  @Inject ConfluentSchemaRegistry confluentSchemaRegistry;
  @Inject SchemaRegistryUtil schemaRegistryUtil;

  @BeforeEach
  void setup() {}

  @Timeout(10)
  @Test
  void testSchema(){
    schemaRegistryUtil.addSchemaVersion(
        "my-topic", KafkaStreamPart.VALUE, "registry/confluent/version1.json");

    final Optional<Schema> valueSchema =
        confluentSchemaRegistry.getLatestSchemaForTopic("my-topic", KafkaStreamPart.VALUE);
    assertThat(valueSchema).isPresent();

    final Optional<Schema> keySchema =
        confluentSchemaRegistry.getLatestSchemaForTopic("my-topic", KafkaStreamPart.KEY);
    assertThat(keySchema).isEmpty();
  }

  @Timeout(10)
  @Test
  void testDeepDiveTool() {
    assertThat(confluentSchemaRegistry.getDeepDiveToolUrl("my-topic"))
        .isPresent()
        .hasValue("https://my-url/my-topic/ui");
  }
}
