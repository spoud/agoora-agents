package io.spoud.agoora.agents.kafka.metrics;

import io.grpc.StatusRuntimeException;
import io.spoud.agoora.agents.api.client.MetricsClient;
import io.spoud.sdm.looker.v1alpha1.ResourceMetric.MetricType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class LookerMetricsService {

  private final MetricsClient metricsClient;

  public void updateMetrics(
      String resourceId,
      MetricsType type,
      Double value,
      Map<String, String> additionalTags) {

    MetricType protoMetricType = MetricType.UNDEFINED;

    if (MetricsType.DATA_PORT_BYTES.equals(type)) {
      protoMetricType = MetricType.DATA_PORT_BYTES;
    } else if (MetricsType.DATA_PORT_MESSAGES_COUNT.equals(type)) {
      protoMetricType = MetricType.DATA_PORT_MESSAGES;
    } else if (MetricsType.DATA_SUBSCRIPTION_STATE_BYTES.equals(type)) {
      protoMetricType = MetricType.DATA_SUBSCRIPTION_STATE_BYTES;
    } else if (MetricsType.DATA_SUBSCRIPTION_STATE_MESSAGES_COUNT.equals(type)) {
      protoMetricType = MetricType.DATA_SUBSCRIPTION_STATE_MESSAGES;
    } else if (MetricsType.DATA_SUBSCRIPTION_STATE_MESSAGES_LAG_COUNT.equals(type)) {
      protoMetricType = MetricType.DATA_SUBSCRIPTION_STATE_MESSAGES_LAG;
    }

    try {
      LOG.debug(
          "Update metrics. ResourceId={}, type={}, value={}, additionalTags",
          resourceId,
          protoMetricType,
          value,
          additionalTags);
      metricsClient.updateMetric(resourceId, protoMetricType, value, additionalTags);
    } catch (StatusRuntimeException e) {
      LOG.error("Error while updating metrics, will skip and continue.", e);
    }
  }
}
