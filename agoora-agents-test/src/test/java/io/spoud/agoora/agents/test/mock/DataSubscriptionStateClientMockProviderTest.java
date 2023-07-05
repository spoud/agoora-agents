package io.spoud.agoora.agents.test.mock;

import com.google.protobuf.StringValue;
import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.DataSubscriptionStateClient;
import io.spoud.sdm.global.selection.v1.IdPathRef;
import io.spoud.sdm.logistics.domain.v1.DataSubscriptionState;
import io.spoud.sdm.logistics.selection.v1.DataPortRef;
import io.spoud.sdm.logistics.service.v1.DataSubscriptionStateChange;
import io.spoud.sdm.logistics.service.v1.SaveDataSubscriptionStateRequest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class DataSubscriptionStateClientMockProviderTest {

  @Inject DataSubscriptionStateClient dataSubscriptionStateClient;

  @Test
  void testClient() {
    final SaveDataSubscriptionStateRequest request =
        SaveDataSubscriptionStateRequest.newBuilder()
            .setInput(
                DataSubscriptionStateChange.newBuilder()
                    .setName(StringValue.of("name"))
                    .setLabel(StringValue.of("label"))
                    .setDataPort(
                        DataPortRef.newBuilder()
                            .setIdPath(IdPathRef.newBuilder().setId("portId").build())
                            .build())
                    .build())
            .build();
    DataSubscriptionState result = dataSubscriptionStateClient.save(request);
    assertThat(result).isNull();

    DataSubscriptionStateClientMockProvider.defaultMock(dataSubscriptionStateClient);
    // test uuid validity
    result = dataSubscriptionStateClient.save(request);
    assertThat(result).isNotNull();
    assertThat(UUID.fromString(result.getId()).toString()).isEqualTo(result.getId());
    assertThat(result.getName()).isEqualTo("name");
    assertThat(result.getLabel()).isEqualTo("label");
    assertThat(result.getDataPort().getId()).isEqualTo("portId");
  }
}
