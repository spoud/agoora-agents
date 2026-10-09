package io.spoud.agoora.agents.test.mock;

import io.spoud.agoora.agents.api.client.TransportClient;
import io.spoud.sdm.logistics.domain.v1.Transport;
import lombok.experimental.UtilityClass;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@UtilityClass
public class TransportClientMockProvider {

  /** Id of the transport every agent under test is managing. */
  public static final String TRANSPORT_ID = "e6b4c0a2-5f0b-4c7e-9a8e-3c1d2b4a5f60";

  public static void defaultMock(TransportClient mock) {
    reset(mock);
    when(mock.getTransport(any())).thenReturn(Transport.newBuilder().setId(TRANSPORT_ID).build());
  }
}
