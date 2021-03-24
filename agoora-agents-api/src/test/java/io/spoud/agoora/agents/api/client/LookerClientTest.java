package io.spoud.agoora.agents.api.client;

import io.spoud.sdm.looker.domain.v1alpha1.DataProfile;
import io.spoud.sdm.looker.v1alpha1.AddDataProfileRequest;
import io.spoud.sdm.looker.v1alpha1.AddDataProfileResponse;
import io.spoud.sdm.looker.v1alpha1.LookerServiceGrpc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LookerClientTest {

  LookerClient lookerClient;
  LookerServiceGrpc.LookerServiceBlockingStub stub;

  @BeforeEach
  void setup() {
    stub = mock(LookerServiceGrpc.LookerServiceBlockingStub.class);
    lookerClient = new LookerClient(stub);
  }

  @Test
  void save() {
    when(stub.addDataProfile(any()))
        .thenReturn(
            AddDataProfileResponse.newBuilder()
                .setDataProfile(DataProfile.newBuilder().setId("id").build())
                .build());

    final DataProfile dataProfile =
        lookerClient.addDataProfile(AddDataProfileRequest.newBuilder().build());

    assertThat(dataProfile.getId()).isEqualTo("id");
  }
}
