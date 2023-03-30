package io.spoud.agoora.agents.api.client;

import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.global.selection.v1.EntityRef;
import io.spoud.sdm.schema.domain.v1alpha.Schema;
import io.spoud.sdm.schema.domain.v1alpha.SchemaEncoding;
import io.spoud.sdm.schema.domain.v1alpha.SchemaSource;
import io.spoud.sdm.schema.v1alpha.SaveSchemaRequest;
import io.spoud.sdm.schema.v1alpha.SaveSchemaResponse;
import io.spoud.sdm.schema.v1alpha.SchemaServiceGrpc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SchemaClientTest {

  SchemaClient schemaClient;
  SchemaServiceGrpc.SchemaServiceBlockingStub stub;

  @BeforeEach
  void setup() {
    stub = mock(SchemaServiceGrpc.SchemaServiceBlockingStub.class);
    schemaClient = new SchemaClient(stub);
  }

  @Test
  void save() {
    when(stub.saveSchema(any()))
        .thenReturn(
            SaveSchemaResponse.newBuilder()
                .setSchema(
                    Schema.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setContent("content")
                        .setEntityRef(
                            EntityRef.newBuilder()
                                .setId("id")
                                .setEntityType(ResourceEntity.Type.DATA_PORT)
                                .build())
                        .setSource(SchemaSource.Type.INFERRED)
                        .setEncoding(SchemaEncoding.Type.JSON)
                        .setKeyEncoding(SchemaEncoding.Type.JSON)
                        .setKeyContent("content")
                        .build())
                .build());

    final Schema schema =
        schemaClient.saveSchema(
            ResourceEntity.Type.DATA_PORT,
            "id",
            "/path/",
            "content",
            SchemaSource.Type.INFERRED,
            SchemaEncoding.Type.JSON,
            "content",
            SchemaEncoding.Type.JSON
        );

    assertThat(schema.getEncoding()).isEqualTo(SchemaEncoding.Type.JSON);
    assertThat(schema.getKeyEncoding()).isEqualTo(SchemaEncoding.Type.JSON);
    assertThat(schema.getSource()).isEqualTo(SchemaSource.Type.INFERRED);

    verify(stub)
        .saveSchema(
            eq(
                SaveSchemaRequest.newBuilder()
                    .setEntityRef(
                        EntityRef.newBuilder().setEntityType(ResourceEntity.Type.DATA_PORT)
                            .setId("id").build())
                    .setPath("/path/")
                    .setSource(SchemaSource.Type.INFERRED)
                    .setEncoding(SchemaEncoding.Type.JSON)
                    .setContent("content")
                    .setKeyContent("content")
                    .setKeyEncoding(SchemaEncoding.Type.JSON)
                    .build()));
  }
}
