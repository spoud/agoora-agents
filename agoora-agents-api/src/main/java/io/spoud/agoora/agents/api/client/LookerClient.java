package io.spoud.agoora.agents.api.client;

import io.spoud.sdm.looker.domain.v1alpha1.DataProfile;
import io.spoud.sdm.looker.v1alpha1.AddDataProfileRequest;
import io.spoud.sdm.looker.v1alpha1.LookerServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class LookerClient {

  private final LookerServiceGrpc.LookerServiceBlockingStub lookerClient;

  public DataProfile addDataProfile(AddDataProfileRequest dataProfileRequest) {
    return lookerClient.addDataProfile(dataProfileRequest).getDataProfile();
  }
}
