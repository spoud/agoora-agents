package io.spoud.agoora.agents.api.auth;

import io.spoud.agoora.agents.api.config.SdmAgentClientAuthConfig;
import io.spoud.agoora.agents.api.config.SdmAgentUserConfig;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.ClientBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthClientInterceptorTest {

  private SdmAgentClientAuthConfig.SdmAgentClientAuthConfigBuilder baseBuilder;

  @BeforeEach
  void setup() {
    baseBuilder =
        SdmAgentClientAuthConfig.builder()
            .serverUrl("http://localhost")
            .realm("spoud")
            .user(SdmAgentUserConfig.builder().name("name").token("token").build());
  }

  @Test
  void testWrongConfig() {
    final SdmAgentClientAuthConfig noServer =
        SdmAgentClientAuthConfig.builder()
            .realm("spoud")
            .user(SdmAgentUserConfig.builder().name("name").token("token").build())
            .build();
    assertThatThrownBy(() -> new AuthClientInterceptor(noServer))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("serverUrl is required");

    final SdmAgentClientAuthConfig noUser =
        SdmAgentClientAuthConfig.builder().serverUrl("http://localhost").realm("spoud").build();
    assertThatThrownBy(() -> new AuthClientInterceptor(noUser))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("user credentials are required");

    final SdmAgentClientAuthConfig noRealm =
        SdmAgentClientAuthConfig.builder()
            .serverUrl("http://localhost")
            .user(SdmAgentUserConfig.builder().name("name").token("token").build())
            .build();
    assertThatThrownBy(() -> new AuthClientInterceptor(noRealm))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("realm is required");
  }

  @Test
  void testNoSsl() {
    final SdmAgentClientAuthConfig config =
        baseBuilder.ignoreSsl(true).trustStoreLocation("blablab").build();

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

    final AuthClientInterceptor authClientInterceptor = new AuthClientInterceptor(baseBuilder.build());

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
