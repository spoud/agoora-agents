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
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

@Slf4j
public class ProfileResponseObserver extends AbstractResponseObserver<ProfileDataStreamResponse> {
  @Getter private ProfilerResponse response = new ProfilerResponse();
  private String profile = "";
  private String schema = "";

  @Override
  protected void accumulator(ProfileDataStreamResponse profileDataStreamResponse) {
    if (profileDataStreamResponse.hasMeta()) {
      final Meta meta = profileDataStreamResponse.getMeta();
      response.setMeta(meta);
      if(StringUtils.isNotBlank(meta.getSchema())){
        schema += meta.getSchema();
      }

    } else {
      profile += profileDataStreamResponse.getProfile();
    }
  }

  @Override
  public void onCompleted() {
    super.onCompleted();
    response.setHtml(profile);
    response.setSchema(schema);

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
