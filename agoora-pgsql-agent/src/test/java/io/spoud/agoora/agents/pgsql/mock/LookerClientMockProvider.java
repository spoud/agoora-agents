package io.spoud.agoora.agents.pgsql.mock;

import io.spoud.agoora.agents.api.client.LookerClient;
import io.spoud.sdm.looker.domain.v1alpha1.DataProfile;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class LookerClientMockProvider {

  public static void defaultMock(LookerClient mock) {
    when(mock.addDataProfile(any())).thenReturn(DataProfile.newBuilder()
            .setId(UUID.randomUUID().toString())
            .build());
  }
}
