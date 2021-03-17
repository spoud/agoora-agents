package io.spoud.agoora.agents.api.observers;

import io.spoud.sdm.profiler.domain.v1alpha1.Meta;
import io.spoud.sdm.profiler.domain.v1alpha1.ProfilerError;
import io.spoud.sdm.profiler.service.v1alpha1.ProfileDataStreamResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ProfileResponseObserver extends AbstractResponseObserver<ProfileDataStreamResponse> {
  @Getter private ProfilerResponse response = new ProfilerResponse();
  private String profile = "";

  @Override
  protected void accumulator(ProfileDataStreamResponse profileDataStreamResponse) {
    if (profileDataStreamResponse.hasMeta()) {
      response.setMeta(profileDataStreamResponse.getMeta());
    } else {
      profile += profileDataStreamResponse.getProfile();
    }
  }

  @Override
  public void onCompleted() {
    super.onCompleted();
    response.setHtml(profile);

    if (!response
        .getMeta()
        .getError()
        .equals(response.getMeta().getError().getDefaultInstanceForType())) {
      this.response.setError(Optional.of(response.getMeta().getError()));
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProfilerResponse {
    private Meta meta;
    private Optional<ProfilerError> error = Optional.empty();
    private String html;
    private String schema;
  }
}
