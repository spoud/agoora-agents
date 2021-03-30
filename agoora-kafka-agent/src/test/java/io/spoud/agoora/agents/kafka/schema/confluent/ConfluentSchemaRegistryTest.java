package io.spoud.agoora.agents.kafka.schema.confluent;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.kafka.AbstractService;
import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import io.spoud.sdm.schema.domain.v1alpha.Schema;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
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
  @RestClient @Inject
  ConfluentRegistrySubjectResource confluentRegistrySubjectResource;

  @BeforeEach
  void setup() {}

  @Timeout(10)
  @Test
  void testSchema() throws IOException {
    addSchemaVersion("my-topic", KafkaStreamPart.VALUE, "registry/confluent/version1.json");

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

  private void addSchemaVersion(String topic, KafkaStreamPart part, String file)
      throws IOException {
    String content =
        IOUtils.toString(
            ConfluentSchemaRegistryTest.class.getClassLoader().getResourceAsStream(file), "UTF-8");
    confluentRegistrySubjectResource.addNewSchemaVersion(topic, part, content);
  }
}
