package io.spoud.agoora.agents.api.client;

import io.spoud.sdm.logistics.domain.v1.DataPort;
import io.spoud.sdm.logistics.service.v1.DataPortServiceGrpc;
import io.spoud.sdm.logistics.service.v1.SaveDataPortRequest;
import io.spoud.sdm.logistics.service.v1.SaveDataPortResponse;
import org.junit.jupiter.api.BeforeEach;
import io.spoud.sdm.global.selection.v1.IdPathRef;
import io.spoud.sdm.global.selection.v1.PageResult;
import io.spoud.sdm.logistics.selection.v1.FilterPredicate;
import io.spoud.sdm.logistics.service.v1.ListDataPortsRequest;
import io.spoud.sdm.logistics.service.v1.ListDataPortsResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

  @Test
  void listAvailableFollowsAllPagesAndFiltersOutDeleted() {
    when(stub.listDataPorts(any()))
        .thenReturn(
            ListDataPortsResponse.newBuilder()
                .addDataPort(DataPort.newBuilder().setId("1"))
                .setPageResult(PageResult.newBuilder().setNextPageToken("next"))
                .build())
        .thenReturn(
            ListDataPortsResponse.newBuilder().addDataPort(DataPort.newBuilder().setId("2")).build());

    final List<DataPort> ports =
        dataPortClient.listAvailable(IdPathRef.newBuilder().setPath("/default/kafka").build());

    assertThat(ports).extracting(DataPort::getId).containsExactly("1", "2");
    ArgumentCaptor<ListDataPortsRequest> captor = ArgumentCaptor.forClass(ListDataPortsRequest.class);
    verify(stub, times(2)).listDataPorts(captor.capture());
    assertThat(captor.getAllValues())
        .extracting(r -> r.getPageRequest().getPageToken())
        .containsExactly("", "next");
    assertThat(captor.getValue().getTransportRef().getIdPath().getPath())
        .isEqualTo("/default/kafka");
    assertThat(captor.getValue().getPredicatesList())
        .extracting(FilterPredicate::getKey, FilterPredicate::getValue)
        .containsExactly(tuple("lifecycleStatus", "CREATED"));
  }
}
