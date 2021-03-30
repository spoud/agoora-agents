package io.spoud.agoora.agents.kafka.schema.confluent;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.kafka.AbstractService;
import io.spoud.sdm.schema.domain.v1alpha.Schema;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class ConfluentSchemaRegistryTest extends AbstractService {

  @Inject ConfluentSchemaRegistry confluentSchemaRegistry;
  @RestClient @Inject ConfluentRegistryResource confluentRegistryResource;

  @BeforeEach
  void setup() {}

  @Timeout(10)
  @Test
  void testSchema() throws IOException {
    addSchemaVersion("my-topic", "registry/confluent/version1.json");

    final List<Schema> schemas = confluentSchemaRegistry.getNewSchemaForTopic("my-topic");
    assertThat(schemas).isNotEmpty();
  }

  @Timeout(10)
  @Test
  void testDeepDiveTool() {
    assertThat(confluentSchemaRegistry.getDeepDiveToolUrl("my-topic"))
        .isPresent()
        .hasValue("https://my-url/my-topic/ui");
  }

  private void addSchemaVersion(String topic, String file) throws IOException {
    String content =
        IOUtils.toString(ConfluentSchemaRegistryTest.class.getClassLoader().getResourceAsStream(file), "UTF-8");
    confluentRegistryResource.addNewSchemaVersion(topic, "value", content);
  }
}
