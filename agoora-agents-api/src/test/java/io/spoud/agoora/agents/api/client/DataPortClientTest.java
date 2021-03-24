package io.spoud.agoora.agents.api.client;

import io.spoud.sdm.logistics.domain.v1.DataPort;
import io.spoud.sdm.logistics.service.v1.DataPortServiceGrpc;
import io.spoud.sdm.logistics.service.v1.SaveDataPortRequest;
import io.spoud.sdm.logistics.service.v1.SaveDataPortResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataPortClientTest {

  DataPortClient dataPortClient;
  DataPortServiceGrpc.DataPortServiceBlockingStub stub;

  @BeforeEach
  void setup() {
    stub = mock(DataPortServiceGrpc.DataPortServiceBlockingStub.class);
    dataPortClient = new DataPortClient(stub);
  }

  @Test
  void save() {
    when(stub.save(any()))
        .thenReturn(
            SaveDataPortResponse.newBuilder()
                .setDataPort(
                    DataPort.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setName("a")
                        .setLabel("b")
                        .build())
                .build());

    final DataPort save = dataPortClient.save(SaveDataPortRequest.newBuilder().build());

    assertThat(save.getName()).isEqualTo("a");
    assertThat(save.getLabel()).isEqualTo("b");
  }
}
