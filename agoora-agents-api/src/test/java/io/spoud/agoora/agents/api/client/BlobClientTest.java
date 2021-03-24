package io.spoud.agoora.agents.api.client;

import io.spoud.agoora.agents.api.observers.AllResponseObserver;
import io.spoud.agoora.agents.api.observers.LastResponseObserver;
import io.spoud.sdm.blob.v1alpha.BlobServiceGrpc;
import io.spoud.sdm.blob.v1alpha.UploadChunkRequest;
import io.spoud.sdm.blob.v1alpha.UploadChunkResponse;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BlobClientTest {

  BlobClient blobClient;
  BlobServiceGrpc.BlobServiceStub stub;

  @BeforeEach
  void setup() {
    stub = mock(BlobServiceGrpc.BlobServiceStub.class);
    blobClient = new BlobClient(stub);
  }

  @Timeout(10)
  @Test
  void testUpload() {
    AllResponseObserver<UploadChunkRequest> streamObserver = new AllResponseObserver<>();

    String id = UUID.randomUUID().toString();
    when(stub.uploadChunk(any()))
        .thenAnswer(
            a -> {
              final LastResponseObserver<UploadChunkResponse> lastResponseObserver =
                  a.getArgument(0, LastResponseObserver.class);
              lastResponseObserver.onNext(UploadChunkResponse.newBuilder().setId(id).build());
              lastResponseObserver.onCompleted();
              return streamObserver;
            });

    String ret = blobClient.uploadBlobUtf8("coucou", "/path/", ResourceEntity.Type.SCHEMA);
    assertThat(ret).isEqualTo(id);

    assertThat(streamObserver.getResponses()).hasSize(2);

    final UploadChunkRequest requestWithMeta = streamObserver.getResponses().get(0);
    assertThat(requestWithMeta.getMeta().getPath()).isEqualTo("/path/");
    assertThat(requestWithMeta.getMeta().getEntity()).isEqualTo(ResourceEntity.Type.SCHEMA);

    final UploadChunkRequest requestWithContent = streamObserver.getResponses().get(1);
    assertThat(requestWithContent.getContent().toString(StandardCharsets.UTF_8))
        .isEqualTo("coucou");
  }
}
