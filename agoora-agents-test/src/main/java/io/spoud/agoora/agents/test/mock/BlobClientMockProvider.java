package io.spoud.agoora.agents.test.mock;

import io.spoud.agoora.agents.api.client.BlobClient;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class BlobClientMockProvider {

  public static void defaultMock( BlobClient mock) {
    when(mock.uploadBlob(any(), any(), any())).thenReturn(UUID.randomUUID().toString());
  }
}
