package io.spoud.agoora.agents.api.client;

import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.global.selection.v1.EntityRef;
import io.spoud.sdm.schema.domain.v1alpha.Schema;
import io.spoud.sdm.schema.domain.v1alpha.SchemaEncoding;
import io.spoud.sdm.schema.domain.v1alpha.SchemaSource;
import io.spoud.sdm.schema.v1alpha.SaveSchemaRequest;
import io.spoud.sdm.schema.v1alpha.SchemaServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SchemaClient {
  private final SchemaServiceGrpc.SchemaServiceBlockingStub stub;

  public Schema saveSchema(
      ResourceEntity.Type entityType,
      String entityId,
      String path,
      String valueSchemaContent,
      SchemaSource.Type source,
      SchemaEncoding.Type valueEncoding,
      String keySchemaContent,
      SchemaEncoding.Type keyEncoding
  ) {
    return stub.saveSchema(
            SaveSchemaRequest.newBuilder()
                .setEntityRef(
                    EntityRef.newBuilder().setEntityType(entityType).setId(entityId).build())
                .setPath(path)
                .setSource(source)
                .setEncoding(valueEncoding)
                .setContent(valueSchemaContent)
                .setKeyEncoding(keyEncoding)
                .setKeyContent(keySchemaContent)
                .build())
        .getSchema();
  }
}
