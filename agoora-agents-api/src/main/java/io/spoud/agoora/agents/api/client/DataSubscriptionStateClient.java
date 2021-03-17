package io.spoud.agoora.agents.api.client;

import io.spoud.sdm.logistics.domain.v1.DataSubscriptionState;
import io.spoud.sdm.logistics.service.v1.DataSubscriptionStateServiceGrpc;
import io.spoud.sdm.logistics.service.v1.SaveDataSubscriptionStateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DataSubscriptionStateClient {
  private final DataSubscriptionStateServiceGrpc.DataSubscriptionStateServiceBlockingStub stub;

  public DataSubscriptionState save(SaveDataSubscriptionStateRequest request) {
    return stub.save(request).getDataSubscriptionState();
  }
}
