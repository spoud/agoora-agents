package io.spoud.agoora.agents.api.client;

import io.spoud.sdm.looker.domain.v1alpha1.ResourceMetric;
import io.spoud.sdm.looker.domain.v1alpha1.ResourceMetricType;
import io.spoud.sdm.looker.v1alpha1.MetricsServiceGrpc;
import io.spoud.sdm.looker.v1alpha1.UpdateMetricRequest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MetricsClientTest {

  MetricsClient metricsClient;
  MetricsServiceGrpc.MetricsServiceBlockingStub stub;

  @BeforeEach
  void setup() {
    stub = mock(MetricsServiceGrpc.MetricsServiceBlockingStub.class);
    metricsClient = new MetricsClient(stub);
  }

  @Test
  void save() {
    String resourceId = "resource";

    metricsClient.updateMetric(
        resourceId, ResourceMetricType.Type.DATA_PORT_ATTRIBUTE_INTEGRITY, 1.0);
    metricsClient.updateMetric(
        resourceId, ResourceMetricType.Type.DATA_PORT_DATASET_SIZE_BYTES, 2.0);

    verify(stub)
        .updateMetric(
            argThat(
                new UpdateMetricRequestMatcher(
                    UpdateMetricRequest.newBuilder()
                        .setMetric(
                            ResourceMetric.newBuilder()
                                .setResourceId(resourceId)
                                .setType(ResourceMetricType.Type.DATA_PORT_ATTRIBUTE_INTEGRITY)
                                .setValue(1.0)
                                .build())
                        .build())));

    verify(stub)
        .updateMetric(
            argThat(
                new UpdateMetricRequestMatcher(
                    UpdateMetricRequest.newBuilder()
                        .setMetric(
                            ResourceMetric.newBuilder()
                                .setResourceId(resourceId)
                                .setType(ResourceMetricType.Type.DATA_PORT_DATASET_SIZE_BYTES)
                                .setValue(2.0)
                                .build())
                        .build())));
  }

  // Matcher that doesn't look at the timestamp
  @RequiredArgsConstructor
  public class UpdateMetricRequestMatcher implements ArgumentMatcher<UpdateMetricRequest> {

    private final UpdateMetricRequest left;

    @Override
    public boolean matches(UpdateMetricRequest right) {
      return left.getMetric().getResourceId().equals(right.getMetric().getResourceId())
          && left.getMetric().getType().equals(right.getMetric().getType())
          && left.getMetric().getValue() == right.getMetric().getValue()
          && left.getMetric().getTagsMap().equals(right.getMetric().getTagsMap());
    }
  }
}
