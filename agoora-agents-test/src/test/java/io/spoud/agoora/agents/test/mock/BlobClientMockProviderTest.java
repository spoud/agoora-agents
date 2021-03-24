package io.spoud.agoora.agents.test.mock;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.BlobClient;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class BlobClientMockProviderTest {

  @Inject BlobClient blobClient;

  @Test
  void testClient() {
    String uuid = blobClient.uploadBlob(new byte[0], "/test/", ResourceEntity.Type.DATA_OFFER);
    assertThat(uuid).isNull();

    BlobClientMockProvider.defaultMock(blobClient);
    // test uuid validity
    uuid = blobClient.uploadBlob(new byte[0], "/test/", ResourceEntity.Type.DATA_OFFER);
    assertThat(UUID.fromString(uuid).toString()).isEqualTo(uuid);
  }
}
