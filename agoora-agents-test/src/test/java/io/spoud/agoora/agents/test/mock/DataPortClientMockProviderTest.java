package io.spoud.agoora.agents.test.mock;

import com.google.protobuf.StringValue;
import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.sdm.logistics.domain.v1.DataPort;
import io.spoud.sdm.logistics.service.v1.DataPortChange;
import io.spoud.sdm.logistics.service.v1.SaveDataPortRequest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class DataPortClientMockProviderTest {

  @Inject DataPortClient dataPortClient;

  @Test
  void testClient() {
    final SaveDataPortRequest request =
        SaveDataPortRequest.newBuilder()
            .setInput(
                DataPortChange.newBuilder()
                    .setName(StringValue.of("name"))
                    .setLabel(StringValue.of("label"))
                    .build())
            .build();
    DataPort result = dataPortClient.save(request);
    assertThat(result).isNull();

    DataPortClientMockProvider.defaultMock(dataPortClient);
    // test uuid validity
    result = dataPortClient.save(request);
    assertThat(result).isNotNull();
    assertThat(UUID.fromString(result.getId()).toString()).isEqualTo(result.getId());
    assertThat(result.getName()).isEqualTo("name");
    assertThat(result.getLabel()).isEqualTo("label");
  }
}
