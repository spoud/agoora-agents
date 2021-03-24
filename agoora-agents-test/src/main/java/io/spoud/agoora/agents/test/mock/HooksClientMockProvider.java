package io.spoud.agoora.agents.test.mock;

import io.spoud.agoora.agents.api.client.HooksClient;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.hooks.domain.v1.LogRecord;
import io.spoud.sdm.hooks.domain.v1.StateChangeAction;
import io.spoud.sdm.logistics.domain.v1.DataPort;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

public class HooksClientMockProvider {

  public static void withLogRecord(HooksClient mock, List<LogRecord> record) {
    doAnswer(
            a -> {
              final Consumer callback = a.getArgument(0, Consumer.class);
              record.forEach(callback::accept);
              return null;
            })
        .when(mock)
        .startListening(any(), anyString(), anyBoolean(), anyBoolean(), anyBoolean());
  }

  public static LogRecord generateDataPortLogRecord(
      StateChangeAction.Type action,
      String id,
      String nameAndLabel,
      String path,
      Map<String, String> properties) {
    return LogRecord.newBuilder()
        .setAction(action)
        .setEntityType(ResourceEntity.Type.DATA_PORT)
        .setEntityUuid(id)
        .setPath(path)
        .setDataPort(
            DataPort.newBuilder()
                .setId(id)
                .setEndpointUrl(nameAndLabel)
                .setLabel(nameAndLabel)
                .setName(nameAndLabel)
                .putAllProperties(properties)
                .build())
        .build();
  }
}
