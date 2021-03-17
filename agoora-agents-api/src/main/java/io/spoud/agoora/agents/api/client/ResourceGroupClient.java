package io.spoud.agoora.agents.api.client;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.spoud.sdm.logistics.domain.v1.ResourceGroup;
import io.spoud.sdm.logistics.selection.v1.ResourceGroupRef;
import io.spoud.sdm.logistics.service.v1.GetResourceGroupRequest;
import io.spoud.sdm.logistics.service.v1.ResourceGroupServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class ResourceGroupClient {
  private final ResourceGroupServiceGrpc.ResourceGroupServiceBlockingStub stub;

  public Optional<ResourceGroup> getResourceGroup(ResourceGroupRef ref) {
    try {
      return Optional.ofNullable(
          stub.getResourceGroup(GetResourceGroupRequest.newBuilder().setSelf(ref).build())
              .getResourceGroup());
    } catch (StatusRuntimeException ex) {
      if (ex.getStatus() == Status.NOT_FOUND) {
        return Optional.empty();
      }
      throw ex;
    }
  }
}
