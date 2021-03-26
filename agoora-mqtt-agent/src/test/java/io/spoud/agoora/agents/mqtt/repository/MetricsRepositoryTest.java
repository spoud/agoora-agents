package io.spoud.agoora.agents.mqtt.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsRepositoryTest {

  MetricsRepository metricsRepository = new MetricsRepository();

  @Test
  void testCounter() {
    assertThat(metricsRepository.addMessagesForDurationAndReturnCounter("1", 1.5)).isEqualTo(1.5);
    assertThat(metricsRepository.addMessagesForDurationAndReturnCounter("2", 3.0)).isEqualTo(3.0);
    assertThat(metricsRepository.addMessagesForDurationAndReturnCounter("2", 2.0)).isEqualTo(5);
    assertThat(metricsRepository.addMessagesForDurationAndReturnCounter("1", 2.0)).isEqualTo(3.5);
  }
}
