package io.spoud.agoora.agents.api.factory;

import io.spoud.agoora.agents.api.config.SdmAgentConfig;
import io.spoud.agoora.agents.api.utils.LazySingletonInstance;
import io.spoud.sdm.blob.v1alpha.BlobServiceGrpc;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BlobClientFactory extends AbstractGrpcClientFactory {

  private final LazySingletonInstance<BlobServiceGrpc.BlobServiceStub> blobServiceStub;

  public BlobClientFactory(SdmAgentConfig config) {
    super(LOG, config.getBlob(), config.getAuth());
    blobServiceStub = new LazySingletonInstance<>(() -> BlobServiceGrpc.newStub(channel.getInstance()));
  }

  public BlobServiceGrpc.BlobServiceStub blobServiceStub() {
    return blobServiceStub.getInstance();
  }
}
