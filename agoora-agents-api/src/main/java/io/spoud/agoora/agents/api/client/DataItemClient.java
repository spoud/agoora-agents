package io.spoud.agoora.agents.api.client;

import io.spoud.sdm.logistics.domain.v1.DataItem;
import io.spoud.sdm.logistics.service.v1.DataItemServiceGrpc;
import io.spoud.sdm.logistics.service.v1.SaveDataItemRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DataItemClient {
  private final DataItemServiceGrpc.DataItemServiceBlockingStub stub;

  public DataItem save(SaveDataItemRequest request) {
    return stub.save(request).getDataItem();
  }
}
