package io.spoud.agoora.agents.kafka.logistics;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.kafka.data.KafkaConsumerGroup;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
class LogisticsServiceTest {
  @Inject LogisticsService logisticsService;

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
}
