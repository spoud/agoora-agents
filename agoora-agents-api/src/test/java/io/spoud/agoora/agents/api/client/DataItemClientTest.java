package io.spoud.agoora.agents.api.client;

import io.spoud.sdm.logistics.domain.v1.DataItem;
import io.spoud.sdm.logistics.service.v1.DataItemServiceGrpc;
import io.spoud.sdm.logistics.service.v1.SaveDataItemRequest;
import io.spoud.sdm.logistics.service.v1.SaveDataItemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataItemClientTest {

  DataItemClient dataItemClient;
  DataItemServiceGrpc.DataItemServiceBlockingStub stub;

  @BeforeEach
  void setup() {
    stub = mock(DataItemServiceGrpc.DataItemServiceBlockingStub.class);
    dataItemClient = new DataItemClient(stub);
  }

  @Test
  void save() {
    when(stub.save(any()))
        .thenReturn(
            SaveDataItemResponse.newBuilder()
                .setDataItem(
                    DataItem.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setName("a")
                        .setLabel("b")
                        .build())
                .build());

    final DataItem save = dataItemClient.save(SaveDataItemRequest.newBuilder().build());

    assertThat(save.getName()).isEqualTo("a");
    assertThat(save.getLabel()).isEqualTo("b");
  }
}
