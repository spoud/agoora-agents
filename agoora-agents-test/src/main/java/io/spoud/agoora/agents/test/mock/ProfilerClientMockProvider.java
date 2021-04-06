package io.spoud.agoora.agents.test.mock;

import io.spoud.agoora.agents.api.client.ProfilerClient;
import io.spoud.agoora.agents.api.observers.ProfileResponseObserver;
import lombok.experimental.UtilityClass;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@UtilityClass
public class ProfilerClientMockProvider {

  public static final String PROFILE = "<html/>";
  public static final String SCHEMA = "Whatever";

  public static void defaultMock(ProfilerClient mock) {
    reset(mock);
    when(mock.profileData(any(), any()))
        .thenReturn(
            ProfileResponseObserver.ProfilerResponse.builder()
                .error(Optional.empty())
                .schema(SCHEMA)
                .html(PROFILE)
                .build());
  }
}
