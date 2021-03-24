package io.spoud.agoora.agents.test.mock;

import io.spoud.agoora.agents.api.client.DataItemClient;
import io.spoud.sdm.global.domain.v1.IdReference;
import io.spoud.sdm.logistics.domain.v1.DataItem;
import io.spoud.sdm.logistics.service.v1.DataItemChange;
import io.spoud.sdm.logistics.service.v1.SaveDataItemRequest;
import lombok.experimental.UtilityClass;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@UtilityClass
public class DataItemClientMockProvider {
  public static void defaultMock(DataItemClient mock) {
    when(mock.save(any()))
        .thenAnswer(
            a -> {
              SaveDataItemRequest request = a.getArgument(0, SaveDataItemRequest.class);
              DataItemChange input = request.getInput();
              return DataItem.newBuilder()
                  .setId(UUID.randomUUID().toString())
                  .setName(input.getName().getValue())
                  .setLabel(input.getLabel().getValue())
                  .setDataPort(
                      IdReference.newBuilder()
                          .setId(input.getDataPortRef().getIdPath().getId())
                          .build())
                  .build();
            });
  }
}
