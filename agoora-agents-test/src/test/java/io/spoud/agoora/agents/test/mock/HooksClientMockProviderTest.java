package io.spoud.agoora.agents.test.mock;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.HooksClient;
import io.spoud.sdm.hooks.domain.v1.LogRecord;
import io.spoud.sdm.hooks.domain.v1.StateChangeAction;
import io.spoud.sdm.hooks.domain.v1.StateChangeFilter;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class HooksClientMockProviderTest {

  @Inject HooksClient hooksClient;

  @Test
  void testClient() {
    final String dataPortId = UUID.randomUUID().toString();
    final String dataSubscriptionStateId = UUID.randomUUID().toString();
    List<LogRecord> initialList =
        Arrays.asList(
            HooksClientMockProvider.generateDataPortLogRecord(
                StateChangeAction.Type.UPDATED, dataPortId, "name", "/path/", Map.of()),
            HooksClientMockProvider.generateDataSubscriptionStateLogRecord(
                StateChangeAction.Type.UPDATED,
                dataSubscriptionStateId,
                dataPortId,
                "name",
                "/path/",
                Map.of()));

    List<LogRecord> consumerRecord = new ArrayList<>();
    final Consumer<LogRecord> consumer = consumerRecord::add;
    final StateChangeFilter filter = StateChangeFilter.newBuilder().build();

    assertThat(hooksClient.startListening(consumer)).isNull();
    assertThat(hooksClient.startListening(consumer, "/path/transport", true, true, true)).isNull();
    assertThat(hooksClient.startListening(consumer, filter)).isNull();
    assertThat(consumerRecord).isEmpty();

    HooksClientMockProvider.withLogRecord(hooksClient, initialList);
    // test uuid validity
    assertThat(hooksClient.startListening(consumer)).isNotNull();
    assertThat(hooksClient.startListening(consumer, "/path/transport", true, true, true))
        .isNotNull();
    assertThat(hooksClient.startListening(consumer, filter)).isNotNull();

    // we register 3 times, we got 3 times the messages
    assertThat(consumerRecord).hasSize(6).containsOnly(initialList.toArray(new LogRecord[0]));
  }
}
