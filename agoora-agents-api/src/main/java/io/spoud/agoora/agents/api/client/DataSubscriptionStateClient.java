package io.spoud.agoora.agents.api.client;

import io.spoud.sdm.global.selection.v1.BaseRef;
import io.spoud.sdm.global.selection.v1.IdPathRef;
import io.spoud.sdm.global.selection.v1.PageRequest;
import io.spoud.sdm.logistics.domain.v1.DataSubscriptionState;
import io.spoud.sdm.logistics.selection.v1.FilterPredicate;
import io.spoud.sdm.logistics.service.v1.DataSubscriptionStateServiceGrpc;
import io.spoud.sdm.logistics.service.v1.ListDataSubscriptionStatesRequest;
import io.spoud.sdm.logistics.service.v1.ListDataSubscriptionStatesResponse;
import io.spoud.sdm.logistics.service.v1.SaveDataSubscriptionStateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class DataSubscriptionStateClient {
  private final DataSubscriptionStateServiceGrpc.DataSubscriptionStateServiceBlockingStub stub;

  public DataSubscriptionState save(SaveDataSubscriptionStateRequest request) {
    return stub.save(request).getDataSubscriptionState();
  }

  /**
   * List all data subscription states of a transport that are not deleted, following every page.
   */
  public List<DataSubscriptionState> listAvailable(IdPathRef transportRef) {
    List<DataSubscriptionState> result = new ArrayList<>();
    String pageToken = "";
    do {
      ListDataSubscriptionStatesResponse response =
          stub.listDataSubscriptionStates(
              ListDataSubscriptionStatesRequest.newBuilder()
                  .setTransportRef(BaseRef.newBuilder().setIdPath(transportRef).build())
                  .addPredicates(
                      FilterPredicate.newBuilder()
                          .setKey(DataPortClient.PREDICATE_LIFECYCLE_STATUS)
                          .setValue(DataPortClient.LIFECYCLE_STATUS_CREATED)
                          .build())
                  .setPageRequest(
                      PageRequest.newBuilder()
                          .setPageSize(DataPortClient.PAGE_SIZE)
                          .setPageToken(pageToken)
                          .build())
                  .build());
      result.addAll(response.getDataSubscriptionStatesList());
      pageToken = response.getPageResult().getNextPageToken();
    } while (!pageToken.isEmpty());
    return result;
  }
}
