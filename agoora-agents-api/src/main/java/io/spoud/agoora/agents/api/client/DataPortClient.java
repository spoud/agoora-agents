package io.spoud.agoora.agents.api.client;

import io.spoud.sdm.global.selection.v1.BaseRef;
import io.spoud.sdm.global.selection.v1.IdPathRef;
import io.spoud.sdm.global.selection.v1.PageRequest;
import io.spoud.sdm.logistics.domain.v1.DataPort;
import io.spoud.sdm.logistics.selection.v1.FilterPredicate;
import io.spoud.sdm.logistics.service.v1.DataPortServiceGrpc;
import io.spoud.sdm.logistics.service.v1.ListDataPortsRequest;
import io.spoud.sdm.logistics.service.v1.ListDataPortsResponse;
import io.spoud.sdm.logistics.service.v1.SaveDataPortRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class DataPortClient {
  public static final String PREDICATE_LIFECYCLE_STATUS = "lifecycleStatus";
  public static final String LIFECYCLE_STATUS_CREATED = "CREATED";
  static final int PAGE_SIZE = 500;

  private final DataPortServiceGrpc.DataPortServiceBlockingStub stub;

  public DataPort save(SaveDataPortRequest request) {
    return stub.save(request).getDataPort();
  }

  /** List all data ports of a transport that are not deleted, following every page. */
  public List<DataPort> listAvailable(IdPathRef transportRef) {
    List<DataPort> result = new ArrayList<>();
    String pageToken = "";
    do {
      ListDataPortsResponse response =
          stub.listDataPorts(
              ListDataPortsRequest.newBuilder()
                  .setTransportRef(BaseRef.newBuilder().setIdPath(transportRef).build())
                  .addPredicates(
                      FilterPredicate.newBuilder()
                          .setKey(PREDICATE_LIFECYCLE_STATUS)
                          .setValue(LIFECYCLE_STATUS_CREATED)
                          .build())
                  .setPageRequest(
                      PageRequest.newBuilder()
                          .setPageSize(PAGE_SIZE)
                          .setPageToken(pageToken)
                          .build())
                  .build());
      result.addAll(response.getDataPortList());
      pageToken = response.getPageResult().getNextPageToken();
    } while (!pageToken.isEmpty());
    return result;
  }
}
