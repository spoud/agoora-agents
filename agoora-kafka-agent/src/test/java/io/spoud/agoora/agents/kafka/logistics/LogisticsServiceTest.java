package io.spoud.agoora.agents.kafka.logistics;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.agoora.agents.api.client.DataSubscriptionStateClient;
import io.spoud.agoora.agents.kafka.data.KafkaConsumerGroup;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import io.spoud.agoora.agents.test.mock.DataPortClientMockProvider;
import io.spoud.agoora.agents.test.mock.DataSubscriptionStateClientMockProvider;
import io.spoud.sdm.logistics.domain.v1.DataPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@QuarkusTest
class LogisticsServiceTest {
  @Inject LogisticsService logisticsService;
  @Inject DataPortClient dataPortClient;
  @Inject DataSubscriptionStateClient dataSubscriptionStateClient;

  @BeforeEach
  void setup() {
    DataPortClientMockProvider.defaultMock(dataPortClient);
    DataSubscriptionStateClientMockProvider.defaultMock(dataSubscriptionStateClient);
    logisticsService.getRefusedRemovals().clear();
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
  void refusedRemovalIsOnlyWarnedOnceAndClearedOnSuccess() {
    KafkaTopic topic = KafkaTopic.builder().dataPortId("refused").topicName("gone").build();
    // logistics refuses writes without permission with NOT_FOUND
    doThrow(new StatusRuntimeException(Status.NOT_FOUND.withDescription("Offer State not found.")))
        .when(dataPortClient)
        .save(any());

    assertThat(logisticsService.deleteDataPort(topic)).isEmpty();
    assertThat(logisticsService.deleteDataPort(topic)).isEmpty();

    // the refusal is warned about only when the id gets added (once), see LogisticsService
    assertThat(logisticsService.getRefusedRemovals()).containsExactly("refused");

    doReturn(DataPort.newBuilder().setId("refused").build()).when(dataPortClient).save(any());
    assertThat(logisticsService.deleteDataPort(topic)).isPresent();
    assertThat(logisticsService.getRefusedRemovals()).isEmpty();
  }

  @Test
  void otherErrorsAreNotTreatedAsRefused() {
    KafkaTopic topic = KafkaTopic.builder().dataPortId("unavailable").topicName("gone").build();
    doThrow(new StatusRuntimeException(Status.UNAVAILABLE)).when(dataPortClient).save(any());

    assertThat(logisticsService.deleteDataPort(topic)).isEmpty();

    assertThat(logisticsService.getRefusedRemovals()).isEmpty();
  }

  @Test
  void refusedSubscriptionStateRemovalIsOnlyWarnedOnce() {
    KafkaConsumerGroup group =
        KafkaConsumerGroup.builder()
            .dataSubscriptionStateId("refused-state")
            .consumerGroupName("group")
            .topicName("gone")
            .build();
    doThrow(new StatusRuntimeException(Status.NOT_FOUND))
        .when(dataSubscriptionStateClient)
        .save(any());

    assertThat(logisticsService.deleteDataSubscriptionState(group)).isEmpty();
    assertThat(logisticsService.deleteDataSubscriptionState(group)).isEmpty();

    assertThat(logisticsService.getRefusedRemovals()).containsExactly("refused-state");
  }
}
