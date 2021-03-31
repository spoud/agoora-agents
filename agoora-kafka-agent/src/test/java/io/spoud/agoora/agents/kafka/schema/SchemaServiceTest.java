package io.spoud.agoora.agents.kafka.schema;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.kafka.AbstractService;
import io.spoud.agoora.agents.kafka.Constants;
import io.spoud.agoora.agents.kafka.utils.SchemaRegistryUtil;
import io.spoud.agoora.agents.test.mock.SchemaClientMockProvider;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.schema.domain.v1alpha.SchemaEncoding;
import io.spoud.sdm.schema.domain.v1alpha.SchemaSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@QuarkusTest
class SchemaServiceTest extends AbstractService {

  @Inject SchemaClient schemaClient;
  @Inject SchemaService schemaService;
  @Inject SchemaRegistryUtil schemaRegistryUtil;

  @BeforeEach
  void setup() {
    SchemaClientMockProvider.defaultMock(schemaClient);
  }

  @Test
  void testWithSchema() {
    schemaRegistryUtil.addSchemaVersion(
        "schema-topic1", KafkaStreamPart.VALUE, "registry/confluent/version1.json");
    final Map<String, String> properties = schemaService.update("schema-topic1", "abc");

    assertThat(properties)
        .containsEntry(
            Constants.PROPETIES_DEEP_DIVE_TOOL_SCHEMA_REGISTRY, "https://my-url/schema-topic1/ui");

    verify(schemaClient)
        .saveSchema(
            eq(ResourceEntity.Type.DATA_PORT),
            eq("abc"),
            eq("/default/"),
            any(),
            eq(SchemaSource.Type.REGISTRY),
            eq(SchemaEncoding.Type.AVRO));
  }

  @Test
  void testWithoutSchema() {
    final Map<String, String> properties = schemaService.update("schema-topicX", "abcd");

    assertThat(properties).isEmpty();

    verifyNoMoreInteractions(schemaClient);
  }
}
