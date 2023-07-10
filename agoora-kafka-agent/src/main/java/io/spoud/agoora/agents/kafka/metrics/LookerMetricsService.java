package io.spoud.agoora.agents.kafka.metrics;

import io.grpc.StatusRuntimeException;
import io.spoud.agoora.agents.api.client.MetricsClient;
import io.spoud.sdm.looker.domain.v1alpha1.ResourceMetricType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class LookerMetricsService {

  private final MetricsClient metricsClient;

  public void updateMetrics(String resourceId, MetricsType type, Double value) {

    ResourceMetricType.Type protoMetricType = ResourceMetricType.Type.UNDEFINED;

    if (MetricsType.DATA_PORT_BYTES.equals(type)) {
      protoMetricType = ResourceMetricType.Type.DATA_PORT_BYTES;
    } else if (MetricsType.DATA_PORT_MESSAGES_COUNT.equals(type)) {
      protoMetricType = ResourceMetricType.Type.DATA_PORT_MESSAGES;
    } else if (MetricsType.DATA_SUBSCRIPTION_STATE_BYTES.equals(type)) {
      protoMetricType = ResourceMetricType.Type.DATA_SUBSCRIPTION_STATE_BYTES;
    } else if (MetricsType.DATA_SUBSCRIPTION_STATE_MESSAGES_COUNT.equals(type)) {
      protoMetricType = ResourceMetricType.Type.DATA_SUBSCRIPTION_STATE_MESSAGES;
    } else if (MetricsType.DATA_SUBSCRIPTION_STATE_MESSAGES_LAG_COUNT.equals(type)) {
      protoMetricType = ResourceMetricType.Type.DATA_SUBSCRIPTION_STATE_MESSAGES_LAG;
    }

    try {
      LOG.debug(
          "Update metrics. ResourceId={}, type={}, value={}", resourceId, protoMetricType, value);
      metricsClient.updateMetric(resourceId, protoMetricType, value);
    } catch (StatusRuntimeException e) {
      LOG.error("Error while updating metrics, will skip and continue.", e);
    }
  }
}
