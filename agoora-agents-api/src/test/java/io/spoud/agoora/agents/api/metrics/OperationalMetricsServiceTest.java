package io.spoud.agoora.agents.api.metrics;

import io.spoud.agoora.agents.api.client.MetricsClient;
import io.spoud.sdm.looker.domain.v1alpha1.ResourceMetricType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OperationalMetricsServiceTest {

  MetricsClient metricsClientMock;
  OperationalMetricsService operationalMetricsService;

  @BeforeEach
  void setup() {
    metricsClientMock = mock(MetricsClient.class);
    operationalMetricsService = new OperationalMetricsService(metricsClientMock);
  }

  @Test
  void testEndWithoutStart() {
    assertThatThrownBy(
            () ->
                operationalMetricsService.iterationEnd(
                    "agent", "/transport/asdf", Duration.ofMinutes(1)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("You should call iterationStart before iterationEnd");
  }

  @Test
  void testMetrics() throws InterruptedException {
    ArgumentCaptor<List<MetricsClient.OperationalMetric>> captor =
        ArgumentCaptor.forClass(List.class);

    operationalMetricsService.iterationStart();
    Thread.sleep(500);
    operationalMetricsService.iterationEnd("agent", "/path/transport", Duration.ofMinutes(1));

    verify(metricsClientMock)
        .updateOperationMetrics(eq("agent"), eq("/path/transport"), captor.capture());

    final List<MetricsClient.OperationalMetric> metrics = captor.getValue();

    assertThat(metrics)
        .extracting(MetricsClient.OperationalMetric::getType)
        .containsExactlyInAnyOrder(
            ResourceMetricType.Type.MEMORY_USAGE_BYTES,
            ResourceMetricType.Type.AGENT_UPTIME_MS,
            ResourceMetricType.Type.LOOP_TIME_MS,
            ResourceMetricType.Type.LOOP_PERCENT,
            ResourceMetricType.Type.LOOPS_COUNTER);

    assertThat(getMetric(metrics, ResourceMetricType.Type.MEMORY_USAGE_BYTES))
        .isBetween(0.0, 1024.0 * 1024 * 1024); // 1gb max

    assertThat(getMetric(metrics, ResourceMetricType.Type.AGENT_UPTIME_MS))
        .isBetween(0.0, 10.0 * 60 * 1000); // 10min max

    assertThat(getMetric(metrics, ResourceMetricType.Type.LOOP_TIME_MS))
        .isBetween(500.0, 1000.0); // 500ms break

    assertThat(getMetric(metrics, ResourceMetricType.Type.LOOP_PERCENT))
        .isBetween(0.5 / 60 * 100, 1.0 / 60 * 100); // 500ms break

    assertThat(getMetric(metrics, ResourceMetricType.Type.LOOPS_COUNTER)).isEqualTo(1);
  }

  private double getMetric(
      List<MetricsClient.OperationalMetric> metrics, ResourceMetricType.Type type) {
    return metrics.stream().filter(m -> m.getType() == type).findFirst().get().getValue();
  }
}
