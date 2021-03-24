package io.spoud.agoora.agents.api.client;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import io.spoud.agoora.agents.api.observers.LastResponseObserver;
import io.spoud.sdm.blob.v1alpha.BlobServiceGrpc;
import io.spoud.sdm.blob.v1alpha.Meta;
import io.spoud.sdm.blob.v1alpha.UploadChunkRequest;
import io.spoud.sdm.blob.v1alpha.UploadChunkResponse;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class BlobClient {

  // 4Mb - 5bytes
  private static final int MAX_SIZE = 4194299;

  private final BlobServiceGrpc.BlobServiceStub blobStub;

  public String uploadBlobUtf8(String utf8String, String path, ResourceEntity.Type type) {
    return uploadBlob(utf8String.getBytes(StandardCharsets.UTF_8), path, type);
  }

  @SneakyThrows
  public String uploadBlob(byte[] blob, String path, ResourceEntity.Type type) {
    LastResponseObserver<UploadChunkResponse> responseObserver = new LastResponseObserver<>();

    final StreamObserver<UploadChunkRequest> uploadChunk = blobStub.uploadChunk(responseObserver);

    uploadChunk.onNext(
        UploadChunkRequest.newBuilder()
            .setMeta(Meta.newBuilder().setPath(path).setEntity(type).build())
            .build());

    int from = 0;
    while (from < blob.length) {
      int to = Math.min(MAX_SIZE, blob.length - from);
      uploadChunk.onNext(
          UploadChunkRequest.newBuilder().setContent(ByteString.copyFrom(blob, from, to)).build());
      from += to;
    }

    uploadChunk.onCompleted();
    responseObserver.awaitCompletion();

    if (responseObserver.getLastResponse() != null
        && responseObserver.getLastResponse().getId() != null) {
      LOG.debug(
          "Successfully added profile with Uuid='{}'", responseObserver.getLastResponse().getId());

      return responseObserver.getLastResponse().getId();
    } else {
      LOG.error("Could not save blob for path: " + path + " blob: " + blob);
      return null;
    }
  }
}
