package io.spoud.agoora.agents.api.factory;

import io.spoud.agoora.agents.api.client.BlobClient;
import io.spoud.agoora.agents.api.client.DataItemClient;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.agoora.agents.api.client.DataSubscriptionStateClient;
import io.spoud.agoora.agents.api.client.HooksClient;
import io.spoud.agoora.agents.api.client.LookerClient;
import io.spoud.agoora.agents.api.client.MetricsClient;
import io.spoud.agoora.agents.api.client.ProfilerClient;
import io.spoud.agoora.agents.api.client.ResourceGroupClient;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.api.client.TransportClient;
import io.spoud.agoora.agents.api.config.SdmAgentClientAuthConfig;
import io.spoud.agoora.agents.api.config.SdmAgentConfig;
import io.spoud.agoora.agents.api.config.SdmAgentEndpointConfig;
import io.spoud.agoora.agents.api.config.SdmAgentUserConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientsFactoryTest {

  public static final String ENDPOINT = "localhost:1234";

  ClientsFactory factory;

  @BeforeEach
  void setup() {
    factory =
        new ClientsFactoryImpl(
            SdmAgentConfig.builder()
                .auth(
                    SdmAgentClientAuthConfig.builder()
                        .realm("realm")
                        .serverUrl("http://localhost/auth")
                        .user(SdmAgentUserConfig.builder().name("name").token("token").build())
                        .build())
                .blob(SdmAgentEndpointConfig.builder().endpoint(ENDPOINT).insecure(true).build())
                .logistics(
                    SdmAgentEndpointConfig.builder().endpoint(ENDPOINT).insecure(true).build())
                .schema(SdmAgentEndpointConfig.builder().endpoint(ENDPOINT).insecure(true).build())
                .profiler(SdmAgentEndpointConfig.builder().endpoint(ENDPOINT).build())
                .looker(SdmAgentEndpointConfig.builder().endpoint(ENDPOINT).build())
                .hooks(SdmAgentEndpointConfig.builder().endpoint(ENDPOINT).build())
                .build());
  }

  @AfterEach
  void tearDown() throws Exception {
    factory.close();
  }

  @Test
  void blob() {
    ClientsFactory factory1 = new ClientsFactoryImpl(SdmAgentConfig.builder().build());

    assertThatThrownBy(() -> factory1.getBlobClient())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("No Endpoint configuration provided");

    ClientsFactory factory2 =
        new ClientsFactoryImpl(
            SdmAgentConfig.builder()
                .blob(SdmAgentEndpointConfig.builder().endpoint(ENDPOINT).build())
                .build());

    assertThat(factory2.getBlobClient()).isNotNull().isInstanceOf(BlobClient.class);
  }

  @Test
  void logistics() {
    assertThat(factory.getDataItemClient()).isNotNull().isInstanceOf(DataItemClient.class);
    assertThat(factory.getDataPortClient()).isNotNull().isInstanceOf(DataPortClient.class);
    assertThat(factory.getDataSubscriptionStateClient())
        .isNotNull()
        .isInstanceOf(DataSubscriptionStateClient.class);
    assertThat(factory.getResourceGroupClient())
        .isNotNull()
        .isInstanceOf(ResourceGroupClient.class);
    assertThat(factory.getTransportClient()).isNotNull().isInstanceOf(TransportClient.class);
  }

  @Test
  void schema() {
    assertThat(factory.getSchemaClient()).isNotNull().isInstanceOf(SchemaClient.class);
  }

  @Test
  void looker() {
    assertThat(factory.getLookerClient()).isNotNull().isInstanceOf(LookerClient.class);
    assertThat(factory.getMetricsClient()).isNotNull().isInstanceOf(MetricsClient.class);
  }

  @Test
  void hooks() {
    assertThat(factory.getHooksClient()).isNotNull().isInstanceOf(HooksClient.class);
  }

  @Test
  void profiler() {
    assertThat(factory.getProfilerClient()).isNotNull().isInstanceOf(ProfilerClient.class);
  }
}
