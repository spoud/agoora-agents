package io.spoud.agoora.agents.api.client;

import io.spoud.agoora.agents.api.observers.AllResponseObserver;
import io.spoud.agoora.agents.api.observers.ProfileResponseObserver;
import io.spoud.sdm.blob.v1alpha.UploadChunkRequest;
import io.spoud.sdm.profiler.domain.v1alpha1.Meta;
import io.spoud.sdm.profiler.service.v1alpha1.ProfileDataStreamResponse;
import io.spoud.sdm.profiler.service.v1alpha1.ProfilerGrpc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfilerClientTest {

  ProfilerClient profilerClient;
  ProfilerGrpc.ProfilerStub stub;

  @BeforeEach
  void setup() {
    stub = mock(ProfilerGrpc.ProfilerStub.class);
    profilerClient = new ProfilerClient(stub);
  }

  @Test
  void profileData() {
    AllResponseObserver<UploadChunkRequest> streamObserver = new AllResponseObserver<>();

    when(stub.profileDataStream(any()))
        .thenAnswer(
            a -> {
              final ProfileResponseObserver responseObserver =
                  a.getArgument(0, ProfileResponseObserver.class);
              responseObserver.onNext(
                  ProfileDataStreamResponse.newBuilder()
                      .setMeta(Meta.newBuilder().setSchema("schema").build())
                      .build());
              responseObserver.onNext(
                  ProfileDataStreamResponse.newBuilder().setProfile("profile").build());
              responseObserver.onCompleted();
              return streamObserver;
            });

    final ProfileResponseObserver.ProfilerResponse response =
        profilerClient.profileData("requestId", Collections.emptyList());

    assertThat(response.getSchema()).isEqualTo("schema");
    assertThat(response.getHtml()).isEqualTo("profile");
  }
}
