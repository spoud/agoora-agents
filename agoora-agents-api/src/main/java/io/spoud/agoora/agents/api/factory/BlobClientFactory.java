package io.spoud.agoora.agents.api.factory;

import io.spoud.agoora.agents.api.config.AgooraAgentConfig;
import io.spoud.agoora.agents.api.utils.LazySingletonInstance;
import io.spoud.sdm.blob.v1alpha.BlobServiceGrpc;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BlobClientFactory extends AbstractGrpcClientFactory {

  private final LazySingletonInstance<BlobServiceGrpc.BlobServiceStub> blobServiceStub;

  public BlobClientFactory(AgooraAgentConfig config) {
    super(LOG, config.blob(), config.auth());
    blobServiceStub = new LazySingletonInstance<>(() -> BlobServiceGrpc.newStub(channel.getInstance()));
  }

  public BlobServiceGrpc.BlobServiceStub blobServiceStub() {
    return blobServiceStub.getInstance();
  }
}
