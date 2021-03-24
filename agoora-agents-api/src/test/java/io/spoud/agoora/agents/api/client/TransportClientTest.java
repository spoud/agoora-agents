package io.spoud.agoora.agents.api.client;

import io.spoud.sdm.global.selection.v1.IdPathRef;
import io.spoud.sdm.logistics.domain.v1.Transport;
import io.spoud.sdm.logistics.service.v1.GetTransportResponse;
import io.spoud.sdm.logistics.service.v1.TransportServiceGrpc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransportClientTest {

  TransportClient transportClient;
  TransportServiceGrpc.TransportServiceBlockingStub stub;

  @BeforeEach
  void setup() {
    stub = mock(TransportServiceGrpc.TransportServiceBlockingStub.class);
    transportClient = new TransportClient(stub);
  }

  @Test
  void getTransport() {
    when(stub.getTransport(any()))
        .thenReturn(
            GetTransportResponse.newBuilder()
                .setTransport(
                    Transport.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setName("a")
                        .setLabel("b")
                        .build())
                .build());

    final Transport transport = transportClient.getTransport(IdPathRef.newBuilder().build());

    assertThat(transport.getName()).isEqualTo("a");
    assertThat(transport.getLabel()).isEqualTo("b");
  }
}
