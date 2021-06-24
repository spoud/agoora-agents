package io.spoud.agoora.agents.test.mock;

import com.google.protobuf.Empty;
import io.spoud.agoora.agents.api.client.MetricsClient;
import lombok.experimental.UtilityClass;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@UtilityClass
public class MetricsClientMockProvider {

  public static void defaultMock(MetricsClient mock) {
    reset(mock);
    when(mock.updateMetric(anyString(), any(), anyDouble())).thenReturn(Empty.getDefaultInstance());
  }
}
