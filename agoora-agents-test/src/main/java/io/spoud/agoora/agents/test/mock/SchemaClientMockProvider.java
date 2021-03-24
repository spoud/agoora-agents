package io.spoud.agoora.agents.test.mock;

import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.api.mapper.StandardProtoMapper;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.global.selection.v1.EntityRef;
import io.spoud.sdm.schema.domain.v1alpha.Schema;
import io.spoud.sdm.schema.domain.v1alpha.SchemaEncoding;
import io.spoud.sdm.schema.domain.v1alpha.SchemaSource;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@UtilityClass
public class SchemaClientMockProvider {

  public static void defaultMock(SchemaClient mock) {
    when(mock.saveSchema(any(), any(), any(), any(), any(), any()))
        .thenAnswer(
            a -> {
              ResourceEntity.Type entityType = a.getArgument(0, ResourceEntity.Type.class);
              String entityId = a.getArgument(1, String.class);
              String path = a.getArgument(2, String.class);
              String schemaContent = a.getArgument(3, String.class);
              SchemaSource.Type source = a.getArgument(4, SchemaSource.Type.class);
              SchemaEncoding.Type encoding = a.getArgument(5, SchemaEncoding.Type.class);

              return Schema.newBuilder()
                  .setId(UUID.randomUUID().toString())
                  .setPath(path)
                  .setContent(schemaContent)
                  .setEntityRef(
                      EntityRef.newBuilder().setEntityType(entityType).setId(entityId).build())
                  .setSource(source)
                  .setEncoding(encoding)
                  .setLastSeen(StandardProtoMapper.timestamp(Instant.now()))
                  .build();
            });
  }
}
