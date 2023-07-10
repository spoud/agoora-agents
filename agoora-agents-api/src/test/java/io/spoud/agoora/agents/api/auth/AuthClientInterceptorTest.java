package io.spoud.agoora.agents.api.auth;

import io.spoud.agoora.agents.api.config.AgooraAgentClientAuthConfig;
import io.spoud.agoora.agents.api.config.AgooraAgentUserConfig;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.net.ssl.SSLContext;
import jakarta.ws.rs.client.ClientBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AuthClientInterceptorTest {

  AgooraAgentClientAuthConfig config;

  AgooraAgentUserConfig user;


  @BeforeEach
  void setup() {
    config = mock(AgooraAgentClientAuthConfig.class);
    user = mock(AgooraAgentUserConfig.class);
    Mockito.when(config.realm()).thenReturn("spoud");
    Mockito.when(config.serverUrl()).thenReturn("http://localhost");
    Mockito.when(config.user()).thenReturn(user);
    Mockito.when(user.name()).thenReturn("name");
    Mockito.when(user.token()).thenReturn("token");
  }

  @Test
  void testWrongConfigServer() {
    final AgooraAgentClientAuthConfig noServer = config;
    Mockito.when(noServer.serverUrl()).thenReturn(null);
    assertThatThrownBy(() -> new AuthClientInterceptor(noServer))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("serverUrl is required");
  }
  @Test
  void testWrongConfigUser() {
    final AgooraAgentClientAuthConfig noUser = config;
    Mockito.when(noUser.user()).thenReturn(null);
    assertThatThrownBy(() -> new AuthClientInterceptor(noUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("user credentials are required");

  }
  @Test
  void testWrongConfigRealm() {
    final AgooraAgentClientAuthConfig noRealm = config;
    Mockito.when(noRealm.realm()).thenReturn(null);
    assertThatThrownBy(() -> new AuthClientInterceptor(noRealm))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("realm is required");
  }

  @Test
  void testNoSsl() {
    final AgooraAgentClientAuthConfig noSslConfig = config;
    Mockito.when(noSslConfig.ignoreSsl()).thenReturn(true);
    Mockito.when(noSslConfig.trustStoreLocation()).thenReturn("blablab");

    final AuthClientInterceptor authClientInterceptor = new AuthClientInterceptor(config);

    final ResteasyClientBuilder clientBuilder = (ResteasyClientBuilder) ClientBuilder.newBuilder();
    authClientInterceptor.configureSslForResteasyClient(clientBuilder);

    assertThat(clientBuilder.getKeyStore()).isNull();
    final SSLContext sslContext = clientBuilder.getSSLContext();
    assertThat(sslContext.getProtocol()).isEqualTo("SSL");
  }

  @Test
  void testProxy() {
    System.getProperties().setProperty("https.proxyHost", "1.2.3.4");
    System.getProperties().setProperty("https.proxyPort", "1234");
    System.getProperties().setProperty("http.proxyHost", "5.6.7.8");
    System.getProperties().setProperty("http.proxyPort", "5678");

    final AuthClientInterceptor authClientInterceptor = new AuthClientInterceptor(config);

    final ResteasyClientBuilder clientBuilder = (ResteasyClientBuilder) ClientBuilder.newBuilder();
    authClientInterceptor.setProxyToResteasyClient(clientBuilder);

    // secure has precedence
    assertThat(clientBuilder.getDefaultProxyHostname()).isEqualTo("1.2.3.4");
    assertThat(clientBuilder.getDefaultProxyPort()).isEqualTo(1234);


    System.getProperties().remove("https.proxyHost");
    System.getProperties().remove("https.proxyPort");

    authClientInterceptor.setProxyToResteasyClient(clientBuilder);

    // fallback to non secure
    assertThat(clientBuilder.getDefaultProxyHostname()).isEqualTo("5.6.7.8");
    assertThat(clientBuilder.getDefaultProxyPort()).isEqualTo(5678);
  }

}
