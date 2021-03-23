package io.spoud.agoora.agents.pgsql.mock;

import io.spoud.agoora.agents.api.client.HooksClient;
import io.spoud.agoora.agents.pgsql.Constants;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.hooks.domain.v1.LogRecord;
import io.spoud.sdm.hooks.domain.v1.StateChangeAction;
import io.spoud.sdm.logistics.domain.v1.DataPort;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

public class HooksClientMockProvider {

  public static void withSomeDataPorts(HooksClient mock, String databaseUrl) {
    withLogRecord(
        mock,
        Arrays.asList(
            generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED, "1", "t_one", "/admin/", databaseUrl),
            generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED, "2", "t_two", "/admin/", databaseUrl),
            generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED, "3", "t_three", "/admin/", databaseUrl),
            generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED, "1", "t_one_e", "/admin/", databaseUrl),
            generateDataPortLogRecord(
                StateChangeAction.Type.DELETED, "2", "t_two", "/admin/", databaseUrl)));
  }

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

  private static LogRecord generateDataPortLogRecord(
      StateChangeAction.Type action, String id, String table, String path, String databaseUrl) {
    return LogRecord.newBuilder()
        .setAction(action)
        .setEntityType(ResourceEntity.Type.DATA_PORT)
        .setEntityUuid(id)
        .setPath(path)
        .setDataPort(
            DataPort.newBuilder()
                .setId(id)
                .setEndpointUrl(table)
                .setLabel(table)
                .setName(table)
                .putAllProperties(
                    Map.of(
                        "prop1",
                        "value1",
                        Constants.SDM_DATABASE_URL,
                        databaseUrl,
                        Constants.SDM_MATCHING_TABLE_NAME,
                        table))
                .build())
        .build();
  }
}
