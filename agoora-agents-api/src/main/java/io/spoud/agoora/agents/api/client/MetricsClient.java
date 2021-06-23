package io.spoud.agoora.agents.api.client;

import com.google.protobuf.Empty;
import io.spoud.agoora.agents.api.mapper.StandardProtoMapper;
import io.spoud.sdm.looker.domain.v1alpha1.AgentRef;
import io.spoud.sdm.looker.domain.v1alpha1.ResourceMetric;
import io.spoud.sdm.looker.domain.v1alpha1.ResourceMetricType;
import io.spoud.sdm.looker.v1alpha1.MetricsServiceGrpc;
import io.spoud.sdm.looker.v1alpha1.UpdateMetricRequest;
import io.spoud.sdm.looker.v1alpha1.UpdateOperationalMetricRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class MetricsClient {
  private final MetricsServiceGrpc.MetricsServiceBlockingStub stub;

  public Empty updateMetric(String resourceId, ResourceMetricType.Type type, double value) {
    return stub.updateMetric(
        UpdateMetricRequest.newBuilder()
            .setMetric(
                ResourceMetric.newBuilder()
                    .setResourceId(resourceId)
                    .setReportTimestamp(StandardProtoMapper.timestamp(Instant.now()))
                    .setType(type)
                    .setValue(value)
                    .build())
            .build());
  }

  public Empty updateOperationMetrics(
      String agentUsername, String transportPath, List<OperationalMetric> metrics) {
    final Instant now = Instant.now();
    return stub.updateOperationalMetric(
        UpdateOperationalMetricRequest.newBuilder()
            .setAgentRef(
                AgentRef.newBuilder()
                    .setAgentUsername(agentUsername)
                    .setTransportPath(transportPath)
                    .build())
            .addAllMetric(
                metrics.stream()
                    .map(
                        m ->
                            io.spoud.sdm.looker.domain.v1alpha1.OperationalMetric.newBuilder()
                                .setReportTimestamp(StandardProtoMapper.timestamp(now))
                                .setType(m.getType())
                                .setValue(m.getValue())
                                .build())
                    .collect(Collectors.toList()))
            .build());
  }

  @Data
  @SuperBuilder
  public static class OperationalMetric {
    private ResourceMetricType.Type type;
    private double value;
  }
}
