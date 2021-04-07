package io.spoud.agoora.agents.kafka.logistics;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.agoora.agents.api.client.DataSubscriptionStateClient;
import io.spoud.agoora.agents.kafka.data.KafkaConsumerGroup;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import io.spoud.agoora.agents.test.mock.DataPortClientMockProvider;
import io.spoud.agoora.agents.test.mock.DataSubscriptionStateClientMockProvider;
import io.spoud.sdm.logistics.domain.v1.DataPort;
import io.spoud.sdm.logistics.domain.v1.DataSubscriptionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
class LogisticsServiceTest {
  @Inject LogisticsService logisticsService;
  @Inject DataPortClient dataPortClient;
  @Inject DataSubscriptionStateClient dataSubscriptionStateClient;

  @BeforeEach
  void setup() {
    DataPortClientMockProvider.defaultMock(dataPortClient);
    DataSubscriptionStateClientMockProvider.defaultMock(dataSubscriptionStateClient);
  }

  @Test
  void testNoDataPortId() {
    assertThatThrownBy(() -> logisticsService.deleteDataPort(KafkaTopic.builder().build()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void testNoDataSubscriptionStateId() {
    assertThatThrownBy(
            () ->
                logisticsService.deleteDataSubscriptionState(KafkaConsumerGroup.builder().build()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void testKafkaTopicProperties() {
    final KafkaTopic kafkaTopic =
        KafkaTopic.builder()
            .topicName("topic-name")
            .dataPortId("dp-id")
            .transportUrl("")
            .properties(Map.of("k1", "v1"))
            .build();
    final DataPort dataPort = logisticsService.updateDataPort(kafkaTopic).get();
    assertThat(dataPort.getPropertiesMap())
        .containsAllEntriesOf(
            Map.of(
                "k1", "v1",
                "sdm.transport.external.kafka.manager.url",
                    "https://km.sdm.spoud.io/clusters/sdm/topics/topic-name",
                "sdm.transport.external.agoora.url", "https://blabla/dp-id"));
  }

  @Test
  void testKafkaConsumerGroupProperties() {
    final KafkaConsumerGroup kafkaConsumerGroup =
        KafkaConsumerGroup.builder()
            .topicName("topic-name")
            .consumerGroupName("consumer-group-name")
            .dataPortId("dp-id")
            .dataSubscriptionStateId("dss-id")
            .transportUrl("")
            .properties(Map.of("k1", "v1"))
            .build();
    final DataSubscriptionState dataSubscriptionState =
        logisticsService.updateDataSubscriptionState(kafkaConsumerGroup).get();
    assertThat(dataSubscriptionState.getPropertiesMap())
        .containsAllEntriesOf(
            Map.of(
                "k1", "v1",
                "sdm.transport.external.kafka.manager.url",
                    "https://km.sdm.spoud.io/clusters/sdm/consumer-group/consumer-group-name/topic-name",
                "sdm.transport.external.agoora.url", "https://blabla/dss-id"));
  }
}
