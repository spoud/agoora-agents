package io.spoud.agoora.agents.api.client;

import io.spoud.sdm.logistics.domain.v1.DataPort;
import io.spoud.sdm.logistics.service.v1.DataPortServiceGrpc;
import io.spoud.sdm.logistics.service.v1.SaveDataPortRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DataPortClient {
  private final DataPortServiceGrpc.DataPortServiceBlockingStub stub;

  public DataPort save(SaveDataPortRequest request) {
    return stub.save(request).getDataPort();
  }
}
