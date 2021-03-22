package io.spoud.agoora.agents.api.auth;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.spoud.agoora.agents.api.auth.token.TokenManager;
import io.spoud.agoora.agents.api.config.SdmAgentClientAuthConfig;
import io.spoud.agoora.agents.api.config.SdmAgentEndpointConfig;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.internal.LocalResteasyProviderFactory;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AuthClientInterceptor implements ClientInterceptor {
  public static final String PROXY_HOST = "http.proxyHost";
  public static final String PROXY_PORT = "http.proxyPort";
  public static final String PROXY_PORT_DEFAULT = "80";
  public static final String PROXY_HOST_SECURE = "https.proxyHost";
  public static final String PROXY_PORT_SECURE = "https.proxyPort";
  public static final String PROXY_PORT_SECURE_DEFAULT = "443";
  public static final String INTEGRATION_CLIENT_ID = "spoud-sdm-integration";

  public static final String META_AUTH_HEADER = "Authorization";

  public static final Metadata.Key<String> AUTH_HEADER_KEY =
      Metadata.Key.of(META_AUTH_HEADER, Metadata.ASCII_STRING_MARSHALLER);

  private final SdmAgentClientAuthConfig authConfig;
  private final SdmAgentEndpointConfig sdmAgentEndpointConfig;

  private final TokenManager tokenManager;

  public AuthClientInterceptor(
      SdmAgentClientAuthConfig authConfig, SdmAgentEndpointConfig sdmAgentEndpointConfig) {
    this.authConfig = authConfig;
    this.sdmAgentEndpointConfig = sdmAgentEndpointConfig;

    ResteasyClient resteasyClient = buildResteasyClient();
    tokenManager = new TokenManager(resteasyClient, authConfig, INTEGRATION_CLIENT_ID);
  }

  @Override
  public <R, A> ClientCall<R, A> interceptCall(
      MethodDescriptor<R, A> method, CallOptions callOptions, Channel next) {

    return new ForwardingClientCall.SimpleForwardingClientCall<>(
        next.newCall(method, callOptions)) {

      @Override
      public void start(Listener<A> responseListener, Metadata headers) {
        try {
          String accessToken = tokenManager.getAccessToken();
          if (accessToken == null) {
            throw new IllegalStateException(
                "JWT token is null, Keycloak agent is not able to retrieve the token");
          }
          headers.put(AUTH_HEADER_KEY, "Bearer " + accessToken);
        } catch (Exception e) {
          LOG.error("Cannot get auth token from auth for accessing endpoint", e);
          super.cancel("Cannot get auth token from auth for accessing endpoint", e);
          return;
        }
        try {
          super.start(responseListener, headers);
        } catch (Exception e) {
          LOG.error("Cannot communicate", e);
          super.cancel("Cannot communicate", e);
          return;
        }
      }
    };
  }

  private ResteasyClient buildResteasyClient() {

    //    final Configuration configuration =
    // ResteasyClientBuilderImpl.newBuilder().getConfiguration();

    //    final ResteasyClientBuilder clientBuilder =
    //            (ResteasyClientBuilder)
    //                    ResteasyClientBuilderImpl.newBuilder().connectTimeout(10,
    // TimeUnit.SECONDS);
    //
    final ResteasyClientBuilder clientBuilder =
        new ResteasyClientBuilderImpl()
            .connectTimeout(10, TimeUnit.SECONDS)
            .withConfig(LocalResteasyProviderFactory.getInstance());

    configureSslForResteasyClient(clientBuilder);
    setProxyToResteasyClient(clientBuilder);

    //          clientBuilder.register(ResteasyJackson2Provider.class, 100);

    return clientBuilder.build();
  }

  private void configureSslForResteasyClient(ResteasyClientBuilder resteasyClientBuilder) {
    if (authConfig.isIgnoreSsl()) {
      LOG.info("Ignoring ssl");
      try {
        TrustManager[] trustAllCerts =
            new TrustManager[] {
              new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                  return null;
                }

                public void checkClientTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {}

                public void checkServerTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {}
              }
            };

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        resteasyClientBuilder.sslContext(sc);
      } catch (Exception ex) {
        throw new RuntimeException("Unable to ignore ssl for resteasy client", ex);
      }
    } else if (authConfig.getTrustStoreLocation() != null
        && !authConfig.getTrustStoreLocation().isBlank()) {
      LOG.info("Using truststore '{}'", authConfig.getTrustStoreLocation());
      try {
        File f = new File(authConfig.getTrustStoreLocation());
        KeyStore trustStore =
            KeyStore.getInstance(f, authConfig.getTrustStorePassword().toCharArray());
        resteasyClientBuilder.trustStore(trustStore);
      } catch (KeyStoreException
          | IOException
          | NoSuchAlgorithmException
          | CertificateException ex) {
        throw new RuntimeException("Unable to configure truststore for resteasy client", ex);
      }
    }
  }

  private void setProxyToResteasyClient(ResteasyClientBuilder resteasyClientBuilder) {
    String proxyHost = null;
    int proxyPort = 80;

    if (System.getProperties().getProperty(PROXY_HOST_SECURE) != null) {
      proxyHost = System.getProperties().getProperty(PROXY_HOST_SECURE);
      proxyPort =
          Integer.parseInt(
              System.getProperties().getProperty(PROXY_PORT_SECURE, PROXY_PORT_SECURE_DEFAULT));
    } else if (System.getProperties().getProperty(PROXY_HOST) != null) {
      proxyHost = System.getProperties().getProperty(PROXY_HOST);
      proxyPort =
          Integer.parseInt(System.getProperties().getProperty(PROXY_PORT, PROXY_PORT_DEFAULT));
    }

    if (proxyHost != null) {
      LOG.info("Using proxy {}:{}", proxyHost, proxyPort);
      resteasyClientBuilder.defaultProxy(proxyHost, proxyPort);
    }
  }
}
