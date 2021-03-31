package io.spoud.agoora.agents.test.mock;

import io.spoud.agoora.agents.api.client.BlobClient;
import lombok.experimental.UtilityClass;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@UtilityClass
public class BlobClientMockProvider {

  public static void defaultMock( BlobClient mock) {
    reset(mock);
    when(mock.uploadBlob(any(), any(), any())).thenReturn(UUID.randomUUID().toString());
  }
}
