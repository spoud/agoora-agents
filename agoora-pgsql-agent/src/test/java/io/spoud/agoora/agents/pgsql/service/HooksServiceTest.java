package io.spoud.agoora.agents.pgsql.service;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.HooksClient;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.pgsql.mock.DataPortClientMockProvider;
import io.spoud.agoora.agents.pgsql.mock.HooksClientMockProvider;
import io.spoud.agoora.agents.pgsql.mock.SchemaClientMockProvider;
import io.spoud.agoora.agents.pgsql.repository.DataPortRepository;
import io.spoud.sdm.logistics.domain.v1.DataPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.reset;

@QuarkusTest()
class HooksServiceTest {

  @Inject DataPortRepository dataPortRepository;
  @Inject ReferenceService referenceService;

  @Inject
  HooksClient hooksClient;

  @Inject DataPortClient dataPortClient;

  @Inject SchemaClient schemaClient;

  @Inject HooksService hooksService;

  @BeforeEach
  void setupMock() {
    HooksClientMockProvider.withSomeDataPorts(hooksClient, referenceService.getDatabaseUrl());
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
