package io.spoud.agoora.agents.test.mock;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.schema.domain.v1alpha.Schema;
import io.spoud.sdm.schema.domain.v1alpha.SchemaEncoding;
import io.spoud.sdm.schema.domain.v1alpha.SchemaSource;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SchemaClientMockProviderTest {

  @Inject SchemaClient schemaClient;

  @Test
  void testClient() {
    UUID entityUuid = UUID.randomUUID();
    Schema schema =
        schemaClient.saveSchema(
            ResourceEntity.Type.DATA_PORT,
            entityUuid.toString(),
            "/path/",
            "content",
            SchemaSource.Type.INFERRED,
            SchemaEncoding.Type.JSON);
    assertThat(schema).isNull();

    SchemaClientMockProvider.defaultMock(schemaClient);
    schema =
        schemaClient.saveSchema(
            ResourceEntity.Type.DATA_PORT,
            entityUuid.toString(),
            "/path/",
            "content",
            SchemaSource.Type.INFERRED,
            SchemaEncoding.Type.JSON);
    assertThat(schema).isNotNull();
    assertThat(UUID.fromString(schema.getId()).toString()).isEqualTo(schema.getId());
    assertThat(schema.getEntityRef().getEntityType()).isEqualTo(ResourceEntity.Type.DATA_PORT);
    assertThat(schema.getEntityRef().getId()).isEqualTo(entityUuid.toString());
    assertThat(schema.getPath()).isEqualTo("/path/");
    assertThat(schema.getContent()).isEqualTo("content");
    assertThat(schema.getSource()).isEqualTo(SchemaSource.Type.INFERRED);
    assertThat(schema.getEncoding()).isEqualTo(SchemaEncoding.Type.JSON);
  }
}
