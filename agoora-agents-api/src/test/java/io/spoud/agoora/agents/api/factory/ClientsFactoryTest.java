package io.spoud.agoora.agents.api.factory;

import io.spoud.agoora.agents.api.client.*;
import io.spoud.agoora.agents.api.config.AgooraAgentClientAuthConfig;
import io.spoud.agoora.agents.api.config.AgooraAgentConfig;
import io.spoud.agoora.agents.api.config.AgooraAgentEndpointConfig;
import io.spoud.agoora.agents.api.config.AgooraAgentUserConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ClientsFactoryTest {

  AgooraAgentConfig config;
  AgooraAgentClientAuthConfig auth;
  AgooraAgentUserConfig user;
  AgooraAgentEndpointConfig blob;
  AgooraAgentEndpointConfig logistics;
  AgooraAgentEndpointConfig schema;
  AgooraAgentEndpointConfig profiler;
  AgooraAgentEndpointConfig looker;
  AgooraAgentEndpointConfig hooks;

  public static final String ENDPOINT = "localhost:1234";

  ClientsFactory factory;

  @BeforeEach
  void setup() {
    config = mock(AgooraAgentConfig.class);
    auth = mock(AgooraAgentClientAuthConfig.class);

    Mockito.when(config.auth()).thenReturn(auth);

    Mockito.when(auth.realm()).thenReturn("realm");
    Mockito.when(auth.serverUrl()).thenReturn("http://localhost/auth");

    user = mock(AgooraAgentUserConfig.class);
    Mockito.when(auth.user()).thenReturn(user);
    Mockito.when(user.name()).thenReturn("name");
    Mockito.when(user.token()).thenReturn("token");

    blob = mock(AgooraAgentEndpointConfig.class);
    Mockito.when(config.blob()).thenReturn(blob);
    Mockito.when(blob.endpoint()).thenReturn(ENDPOINT);
    Mockito.when(blob.insecure()).thenReturn(true);

    logistics = mock(AgooraAgentEndpointConfig.class);
    Mockito.when(config.logistics()).thenReturn(logistics);
    Mockito.when(logistics.endpoint()).thenReturn(ENDPOINT);
    Mockito.when(logistics.insecure()).thenReturn(true);

    schema = mock(AgooraAgentEndpointConfig.class);
    Mockito.when(config.schema()).thenReturn(schema);
    Mockito.when(schema.endpoint()).thenReturn(ENDPOINT);
    Mockito.when(schema.insecure()).thenReturn(true);

    profiler = mock(AgooraAgentEndpointConfig.class);
    Mockito.when(config.profiler()).thenReturn(profiler);
    Mockito.when(profiler.endpoint()).thenReturn(ENDPOINT);
    Mockito.when(profiler.insecure()).thenReturn(true);

    looker = mock(AgooraAgentEndpointConfig.class);
    Mockito.when(config.looker()).thenReturn(looker);
    Mockito.when(looker.endpoint()).thenReturn(ENDPOINT);
    Mockito.when(looker.insecure()).thenReturn(true);

    hooks = mock(AgooraAgentEndpointConfig.class);
    Mockito.when(config.hooks()).thenReturn(hooks);
    Mockito.when(hooks.endpoint()).thenReturn(ENDPOINT);
    Mockito.when(hooks.insecure()).thenReturn(true);

    factory =
        new ClientsFactoryImpl(config);
  }

  @AfterEach
  void tearDown() throws Exception {
    factory.close();
  }

  @Test
  void blob() {
    Mockito.when(config.blob()).thenReturn(null);
    ClientsFactory factory1 = new ClientsFactoryImpl(config);

    assertThatThrownBy(() -> factory1.getBlobClient())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("No Endpoint configuration provided");

    blob = mock(AgooraAgentEndpointConfig.class);
    Mockito.when(config.blob()).thenReturn(blob);
    Mockito.when(blob.endpoint()).thenReturn(ENDPOINT);
    Mockito.when(blob.insecure()).thenReturn(true);
    ClientsFactory factory2 =
        new ClientsFactoryImpl(config);

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
