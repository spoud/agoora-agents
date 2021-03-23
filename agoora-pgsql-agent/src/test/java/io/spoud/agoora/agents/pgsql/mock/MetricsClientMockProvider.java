package io.spoud.agoora.agents.pgsql.mock;

import io.spoud.agoora.agents.api.client.MetricsClient;
import io.spoud.sdm.looker.v1alpha1.UpdateMetricResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class MetricsClientMockProvider {

  public static void defaultMock(MetricsClient mock) {
    when(mock.updateMetric(anyString(), any(), anyDouble()))
        .thenReturn(UpdateMetricResponse.newBuilder().build());
  }
}
