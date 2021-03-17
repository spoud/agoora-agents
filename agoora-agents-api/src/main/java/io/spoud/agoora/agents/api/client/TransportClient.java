package io.spoud.agoora.agents.api.client;

import io.spoud.sdm.global.selection.v1.BaseRef;
import io.spoud.sdm.global.selection.v1.IdPathRef;
import io.spoud.sdm.logistics.domain.v1.Transport;
import io.spoud.sdm.logistics.service.v1.GetTransportRequest;
import io.spoud.sdm.logistics.service.v1.TransportServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TransportClient {
  private final TransportServiceGrpc.TransportServiceBlockingStub stub;

  public Transport getTransport(IdPathRef transportRef) {
    return stub.getTransport(
            GetTransportRequest.newBuilder()
                .setSelf(BaseRef.newBuilder().setIdPath(transportRef).build())
                .build())
        .getTransport();
  }
}
