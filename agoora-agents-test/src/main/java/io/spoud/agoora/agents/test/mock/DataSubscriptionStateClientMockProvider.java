package io.spoud.agoora.agents.test.mock;

import io.spoud.agoora.agents.api.client.DataSubscriptionStateClient;
import io.spoud.sdm.global.domain.v1.IdReference;
import io.spoud.sdm.logistics.domain.v1.DataSubscriptionState;
import io.spoud.sdm.logistics.service.v1.DataSubscriptionStateChange;
import io.spoud.sdm.logistics.service.v1.SaveDataSubscriptionStateRequest;
import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@UtilityClass
public class DataSubscriptionStateClientMockProvider {

  public static Map<String, UUID> uuidByLabel = new HashMap<>();

  public static UUID lastUuid;

  public static void defaultMock(DataSubscriptionStateClient mock) {
    reset(mock);
    when(mock.save(any()))
        .thenAnswer(
            a -> {
              lastUuid = UUID.randomUUID();
              SaveDataSubscriptionStateRequest request =
                  a.getArgument(0, SaveDataSubscriptionStateRequest.class);
              DataSubscriptionStateChange input = request.getInput();
              uuidByLabel.put(input.getLabel().getValue(), lastUuid);
              return DataSubscriptionState.newBuilder()
                  .setId(lastUuid.toString())
                  .setName(input.getName().getValue())
                  .setDataPort(
                      IdReference.newBuilder()
                          .setId(input.getDataPort().getIdPath().getId())
                          .build())
                  .setLabel(input.getLabel().getValue())
                  .setTransportUrl(input.getTransportUrl().getValue())
                  .putAllProperties(input.getProperties().getPropertiesMap())
                  .build();
            });
  }
}
