package io.spoud.agoora.agents.api.client;

import io.spoud.agoora.agents.api.mapper.StandardProtoMapper;
import io.spoud.sdm.looker.v1alpha1.MetricsServiceGrpc;
import io.spoud.sdm.looker.v1alpha1.ResourceMetric;
import io.spoud.sdm.looker.v1alpha1.UpdateMetricRequest;
import io.spoud.sdm.looker.v1alpha1.UpdateMetricResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class MetricsClient {
  private final MetricsServiceGrpc.MetricsServiceBlockingStub stub;

  public UpdateMetricResponse updateMetric(
      String resourceId, ResourceMetric.MetricType type, double value) {
    return updateMetric(resourceId, type, value, Collections.emptyMap());
  }

  public UpdateMetricResponse updateMetric(
      String resourceId, ResourceMetric.MetricType type, double value, Map<String, String> tags) {
    return stub.updateMetric(
        UpdateMetricRequest.newBuilder()
            .setMetric(
                ResourceMetric.newBuilder()
                    .setResourceId(resourceId)
                    .setReportTimestamp(StandardProtoMapper.timestamp(Instant.now()))
                    .setType(type)
                    .setValue(value)
                    .putAllTags(tags)
                    .build())
            .build());
  }
}
