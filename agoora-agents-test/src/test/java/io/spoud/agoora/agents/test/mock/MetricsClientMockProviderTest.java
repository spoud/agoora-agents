package io.spoud.agoora.agents.test.mock;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.MetricsClient;
import io.spoud.sdm.looker.v1alpha1.ResourceMetric;
import io.spoud.sdm.looker.v1alpha1.UpdateMetricResponse;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class MetricsClientMockProviderTest {

  @Inject MetricsClient metricsClient;

  @Test
  void testClient() {
    final String resourceId = UUID.randomUUID().toString();
    UpdateMetricResponse result =
        metricsClient.updateMetric(
            resourceId, ResourceMetric.MetricType.DATA_PORT_ATTRIBUTE_INTEGRITY, 1.0);
    assertThat(result).isNull();

    MetricsClientMockProvider.defaultMock(metricsClient);
    // test uuid validity
    result =
        metricsClient.updateMetric(
            resourceId, ResourceMetric.MetricType.DATA_PORT_ATTRIBUTE_INTEGRITY, 1.0);
    assertThat(result).isNotNull();
  }
}
