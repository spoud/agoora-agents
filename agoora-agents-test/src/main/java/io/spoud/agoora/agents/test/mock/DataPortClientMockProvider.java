package io.spoud.agoora.agents.test.mock;

import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.sdm.logistics.domain.v1.DataPort;
import io.spoud.sdm.logistics.service.v1.DataPortChange;
import io.spoud.sdm.logistics.service.v1.SaveDataPortRequest;
import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@UtilityClass
public class DataPortClientMockProvider {

  public static Map<String, UUID> uuidByLabel = new HashMap<>();

  public static UUID lastUuid;

  public static void defaultMock(DataPortClient mock) {
    when(mock.save(any()))
        .thenAnswer(
            a -> {
              lastUuid = UUID.randomUUID();
              SaveDataPortRequest request = a.getArgument(0, SaveDataPortRequest.class);
              DataPortChange input = request.getInput();
              uuidByLabel.put(input.getLabel().getValue(), lastUuid);
              return DataPort.newBuilder()
                  .setId(lastUuid.toString())
                  .setName(input.getName().getValue())
                  .setLabel(input.getLabel().getValue())
                  .setEndpointUrl(input.getTransportUrl().getValue())
                  .putAllProperties(input.getProperties().getPropertiesMap())
                  .build();
            });
  }
}
