package io.spoud.agoora.agents.api.client;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.spoud.sdm.logistics.domain.v1.ResourceGroup;
import io.spoud.sdm.logistics.selection.v1.ResourceGroupRef;
import io.spoud.sdm.logistics.service.v1.GetResourceGroupResponse;
import io.spoud.sdm.logistics.service.v1.ResourceGroupServiceGrpc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

class ResourceGroupClientTest {

  ResourceGroupClient resourceGroupClient;
  ResourceGroupServiceGrpc.ResourceGroupServiceBlockingStub stub;

  @BeforeEach
  void setup() {
    stub = mock(ResourceGroupServiceGrpc.ResourceGroupServiceBlockingStub.class);
    resourceGroupClient = new ResourceGroupClient(stub);
  }

  @AfterEach
  void tearDown(){
    reset(stub);
  }

  @Test
  void getResourceGroup() {
    when(stub.getResourceGroup(any()))
        .thenReturn(
            GetResourceGroupResponse.newBuilder()
                .setResourceGroup(
                    ResourceGroup.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setName("a")
                        .setLabel("b")
                        .build())
                .build());

    final Optional<ResourceGroup> resourceGroup =
        resourceGroupClient.getResourceGroup(ResourceGroupRef.newBuilder().build());

    assertThat(resourceGroup).isPresent();
    assertThat(resourceGroup.get().getName()).isEqualTo("a");
    assertThat(resourceGroup.get().getLabel()).isEqualTo("b");
  }

  @Test
  void testNotFound() {
    when(stub.getResourceGroup(any())).thenThrow(new StatusRuntimeException(Status.NOT_FOUND));

    final Optional<ResourceGroup> resourceGroup =
        resourceGroupClient.getResourceGroup(ResourceGroupRef.newBuilder().build());

    assertThat(resourceGroup).isEmpty();
  }
}
