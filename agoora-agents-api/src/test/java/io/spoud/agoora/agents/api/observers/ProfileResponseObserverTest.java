package io.spoud.agoora.agents.api.observers;

import io.spoud.sdm.profiler.domain.v1alpha1.Meta;
import io.spoud.sdm.profiler.service.v1alpha1.ProfileDataStreamResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ProfileResponseObserverTest {

  ProfileResponseObserver profileResponseObserver;

  @BeforeEach
  void setup() {
    profileResponseObserver = new ProfileResponseObserver();
  }

  @Test
  void testProfilerResponseObserver() {
    assertThat(profileResponseObserver.getResponse()).isNotNull();
    assertThat(profileResponseObserver.getResponse().getMeta()).isNull();
    assertThat(profileResponseObserver.getResponse().getHtml()).isNull();
    assertThat(profileResponseObserver.getResponse().getError()).isEmpty();
    assertThat(profileResponseObserver.getResponse().getSchema()).isNull();

    profileResponseObserver.onNext(
        ProfileDataStreamResponse.newBuilder()
            .setProfile("a")
            .setMeta(Meta.newBuilder().setSchema("schema").build())
            .build());
    // not completed yet
    assertThat(profileResponseObserver.getResponse()).isNotNull();
    assertThat(profileResponseObserver.getResponse().getMeta())
        .isEqualTo(Meta.newBuilder().setSchema("schema").build());
    assertThat(profileResponseObserver.getResponse().getHtml()).isNull();
    assertThat(profileResponseObserver.getResponse().getError()).isEmpty();
    assertThat(profileResponseObserver.getResponse().getSchema()).isNull();

    profileResponseObserver.onNext(ProfileDataStreamResponse.newBuilder().setProfile("b").build());
    profileResponseObserver.onNext(ProfileDataStreamResponse.newBuilder().setProfile("c").build());
    profileResponseObserver.onCompleted();

    // don't take in account the profile when there is meta
    assertThat(profileResponseObserver.getResponse().getSchema()).isEqualTo("schema");
    assertThat(profileResponseObserver.getResponse().getHtml()).isEqualTo("bc");

    assertThat(profileResponseObserver.getError()).isNull();
  }

  @Test
  void testError() {
    assertThat(profileResponseObserver.getError()).isNull();
    profileResponseObserver.onError(new RuntimeException("a"));
    assertThat(profileResponseObserver.getError())
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("a");
    profileResponseObserver.onError(new RuntimeException("b"));
    assertThat(profileResponseObserver.getError())
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("b");
  }
}
