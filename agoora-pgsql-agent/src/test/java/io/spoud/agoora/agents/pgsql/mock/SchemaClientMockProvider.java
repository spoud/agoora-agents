package io.spoud.agoora.agents.pgsql.mock;

import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.sdm.schema.domain.v1alpha.Schema;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SchemaClientMockProvider {

  public static void defaultMock(SchemaClient mock) {
    when(mock.saveSchema(any(), any(), any(), any(), any(), any()))
        .thenReturn(Schema.newBuilder().setId(UUID.randomUUID().toString()).build());
  }
}
