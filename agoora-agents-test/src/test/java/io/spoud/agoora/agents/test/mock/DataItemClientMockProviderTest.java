package io.spoud.agoora.agents.test.mock;

import com.google.protobuf.StringValue;
import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.DataItemClient;
import io.spoud.sdm.logistics.domain.v1.DataItem;
import io.spoud.sdm.logistics.service.v1.DataItemChange;
import io.spoud.sdm.logistics.service.v1.SaveDataItemRequest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class DataItemClientMockProviderTest {

  @Inject DataItemClient dataItemClient;

  @Test
  void testClient() {
    final SaveDataItemRequest request =
        SaveDataItemRequest.newBuilder()
            .setInput(
                DataItemChange.newBuilder()
                    .setName(StringValue.of("name"))
                    .setLabel(StringValue.of("label"))
                    .build())
            .build();
    DataItem result = dataItemClient.save(request);
    assertThat(result).isNull();

    DataItemClientMockProvider.defaultMock(dataItemClient);
    // test uuid validity
    result = dataItemClient.save(request);
    assertThat(result).isNotNull();
    assertThat(UUID.fromString(result.getId()).toString()).isEqualTo(result.getId());
    assertThat(result.getName()).isEqualTo("name");
    assertThat(result.getLabel()).isEqualTo("label");
  }
}
