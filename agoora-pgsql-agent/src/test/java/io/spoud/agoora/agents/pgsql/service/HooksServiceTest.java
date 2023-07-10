package io.spoud.agoora.agents.pgsql.service;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.agoora.agents.api.client.HooksClient;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.pgsql.Constants;
import io.spoud.agoora.agents.pgsql.repository.DataPortRepository;
import io.spoud.agoora.agents.test.mock.DataPortClientMockProvider;
import io.spoud.agoora.agents.test.mock.HooksClientMockProvider;
import io.spoud.agoora.agents.test.mock.SchemaClientMockProvider;
import io.spoud.sdm.hooks.domain.v1.LogRecord;
import io.spoud.sdm.hooks.domain.v1.StateChangeAction;
import io.spoud.sdm.logistics.domain.v1.DataPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.reset;

@QuarkusTest()
class HooksServiceTest {

  @Inject DataPortRepository dataPortRepository;
  @Inject ReferenceService referenceService;

  @Inject HooksClient hooksClient;

  @Inject DataPortClient dataPortClient;

  @Inject SchemaClient schemaClient;

  @Inject HooksService hooksService;

  public static LogRecord generateDataPortLogRecord(
      StateChangeAction.Type action, String id, String table, String path, String databaseUrl) {
    final Map<String, String> properties =
        Map.of(
            "prop1",
            "value1",
            Constants.AGOORA_DATABASE_URL,
            databaseUrl,
            Constants.AGOORA_MATCHING_TABLE_NAME,
            table);
    return HooksClientMockProvider.generateDataPortLogRecord(action, id, table, path, properties);
  }

  @BeforeEach
  void setupMock() {

    HooksClientMockProvider.withLogRecord(
        hooksClient,
        Arrays.asList(
            generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED,
                "1",
                "t_one",
                "/admin/",
                referenceService.getDatabaseUrl()),
            generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED,
                "2",
                "t_two",
                "/admin/",
                referenceService.getDatabaseUrl()),
            generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED,
                "3",
                "t_three",
                "/admin/",
                referenceService.getDatabaseUrl()),
            generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED,
                "1",
                "t_one_e",
                "/admin/",
                referenceService.getDatabaseUrl()),
            generateDataPortLogRecord(
                StateChangeAction.Type.DELETED,
                "2",
                "t_two",
                "/admin/",
                referenceService.getDatabaseUrl())));

    DataPortClientMockProvider.defaultMock(dataPortClient);
    SchemaClientMockProvider.defaultMock(schemaClient);
    hooksService.startListeningToHooks();
  }

  @AfterEach
  public void tearDown() {
    reset(hooksClient);
    reset(dataPortClient);
    reset(schemaClient);
    dataPortRepository.clear();
  }

  @Test
  void testHooks() {
    await().atMost(Duration.ofSeconds(10)).until(() -> dataPortRepository.findAll().size() >= 2);
    List<DataPort> list = dataPortRepository.findAll();
    assertThat(list).extracting(DataPort::getEndpointUrl).containsExactly("t_one_e", "t_three");
  }
}
