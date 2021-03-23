package io.spoud.agoora.agents.pgsql.mock;

import io.spoud.agoora.agents.api.client.ProfilerClient;
import io.spoud.agoora.agents.api.observers.ProfileResponseObserver;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ProfilerClientMockProvider {

  public static void defaultMock(ProfilerClient mock) {
    when(mock.profileData(any(), any()))
        .thenReturn(
            ProfileResponseObserver.ProfilerResponse.builder()
                .error(Optional.empty())
                .html("<html/>")
                .build());
  }
}
