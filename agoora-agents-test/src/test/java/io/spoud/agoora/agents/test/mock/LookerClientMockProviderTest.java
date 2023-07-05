package io.spoud.agoora.agents.test.mock;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.LookerClient;
import io.spoud.sdm.looker.domain.v1alpha1.DataProfile;
import io.spoud.sdm.looker.v1alpha1.AddDataProfileRequest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class LookerClientMockProviderTest {

    @Inject
    LookerClient lookerClient;

    @Test
    void testClient() {
        DataProfile result = lookerClient.addDataProfile(AddDataProfileRequest.newBuilder().build());
        assertThat(result).isNull();

        LookerClientMockProvider.defaultMock(lookerClient);
        // test uuid validity
        result = lookerClient.addDataProfile(AddDataProfileRequest.newBuilder().build());
        assertThat(UUID.fromString(result.getId()).toString()).isEqualTo(result.getId());
    }

}
