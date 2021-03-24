package io.spoud.agoora.agents.test.mock;

import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.sdm.logistics.domain.v1.DataPort;
import io.spoud.sdm.logistics.service.v1.DataPortChange;
import io.spoud.sdm.logistics.service.v1.SaveDataPortRequest;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class DataPortClientMockProvider {

  public static UUID lastUuid;

  public static void defaultMock(DataPortClient mock) {
    when(mock.save(any()))
        .thenAnswer(
            a -> {
              lastUuid = UUID.randomUUID();
              SaveDataPortRequest request = a.getArgument(0, SaveDataPortRequest.class);
              DataPortChange input = request.getInput();
              return DataPort.newBuilder()
                  .setId(lastUuid.toString())
                  .setName(input.getName().getValue())
                  .setLabel(input.getLabel().getValue())
                  .setEndpointUrl(input.getTransportUrl().getValue())
                  .build();
            });
  }
}
