package io.spoud.agoora.agents.openapi.service;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.agoora.agents.api.client.HooksClient;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.openapi.repository.DataPortRepository;
import io.spoud.agoora.agents.test.mock.DataPortClientMockProvider;
import io.spoud.agoora.agents.test.mock.HooksClientMockProvider;
import io.spoud.agoora.agents.test.mock.SchemaClientMockProvider;
import io.spoud.sdm.hooks.domain.v1.StateChangeAction;
import io.spoud.sdm.logistics.domain.v1.DataPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

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

  public static void withSomeDataPorts(HooksClient mock) {
    HooksClientMockProvider.withLogRecord(
        mock,
        Arrays.asList(
            HooksClientMockProvider.generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED, "1", "t_one", "/admin/"),
            HooksClientMockProvider.generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED, "2", "t_two", "/admin/"),
            HooksClientMockProvider.generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED, "3", "t_three", "/admin/"),
            HooksClientMockProvider.generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED, "1", "t_one_e", "/admin/"),
            HooksClientMockProvider.generateDataPortLogRecord(
                StateChangeAction.Type.DELETED, "2", "t_two", "/admin/")));
  }

  @BeforeEach
  void setupMock() {
    withSomeDataPorts(hooksClient);
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
