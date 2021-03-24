package io.spoud.agoora.agents.api.client;

import io.spoud.sdm.logistics.domain.v1.DataSubscriptionState;
import io.spoud.sdm.logistics.service.v1.DataSubscriptionStateServiceGrpc;
import io.spoud.sdm.logistics.service.v1.SaveDataSubscriptionStateRequest;
import io.spoud.sdm.logistics.service.v1.SaveDataSubscriptionStateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataSubscriptionStateClientTest {

  DataSubscriptionStateClient dataSubscriptionStateClient;
  DataSubscriptionStateServiceGrpc.DataSubscriptionStateServiceBlockingStub stub;

  @BeforeEach
  void setup() {
    stub = mock(DataSubscriptionStateServiceGrpc.DataSubscriptionStateServiceBlockingStub.class);
    dataSubscriptionStateClient = new DataSubscriptionStateClient(stub);
  }

  @Test
  void save() {
    when(stub.save(any()))
        .thenReturn(
            SaveDataSubscriptionStateResponse.newBuilder()
                .setDataSubscriptionState(
                    DataSubscriptionState.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setName("a")
                        .setLabel("b")
                        .build())
                .build());

    final DataSubscriptionState save =
        dataSubscriptionStateClient.save(SaveDataSubscriptionStateRequest.newBuilder().build());

    assertThat(save.getName()).isEqualTo("a");
    assertThat(save.getLabel()).isEqualTo("b");
  }
}
