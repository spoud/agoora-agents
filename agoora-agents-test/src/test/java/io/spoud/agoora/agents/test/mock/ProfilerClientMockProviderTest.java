package io.spoud.agoora.agents.test.mock;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.ProfilerClient;
import io.spoud.agoora.agents.api.observers.ProfileResponseObserver;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class ProfilerClientMockProviderTest {

    @Inject
    ProfilerClient profilerClient;

    @Test
    void testClient() {
        ProfileResponseObserver.ProfilerResponse result = profilerClient.profileData("requestId", Collections.emptyList());
        assertThat(result).isNull();

        ProfilerClientMockProvider.defaultMock(profilerClient);
        // test uuid validity
        result = profilerClient.profileData("requestId", Collections.emptyList());
        assertThat(result).isNotNull();
        assertThat(result.getError()).isEmpty();
        assertThat(result.getHtml()).isEqualTo("<html/>");
    }

}
