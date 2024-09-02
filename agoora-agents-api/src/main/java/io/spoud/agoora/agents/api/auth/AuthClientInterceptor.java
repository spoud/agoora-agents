package io.spoud.agoora.agents.api.auth;

import io.grpc.*;
import io.quarkus.grpc.GlobalInterceptor;
import io.spoud.agoora.agents.api.config.AgooraAgentClientAuthConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.JacksonProvider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

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
@GlobalInterceptor
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

  private final AgooraAgentClientAuthConfig authConfig;
  private final Keycloak keycloakClient;

  public AuthClientInterceptor(AgooraAgentClientAuthConfig authConfig) {
    this.authConfig = authConfig;

    if (authConfig.user() == null
        || authConfig.user().name() == null
        || authConfig.user().token() == null) {
      throw new IllegalArgumentException("user credentials are required");
    }

    if (authConfig.realm() == null) {
      throw new IllegalArgumentException("realm is required");
    }

    if (authConfig.serverUrl() == null) {
      throw new IllegalArgumentException("serverUrl is required");
    }

    final KeycloakBuilder builder =
        KeycloakBuilder.builder()
            .serverUrl(authConfig.serverUrl())
            .realm(authConfig.realm())
            .clientId(INTEGRATION_CLIENT_ID)
            .grantType(OAuth2Constants.PASSWORD)
            .username(authConfig.user().name())
            .password(authConfig.user().token());
    try {
      ResteasyClient resteasyClient = buildResteasyClient();
      builder.resteasyClient(resteasyClient);
    } catch (Exception ex) {
      LOG.error("Error while configuring resteasy", ex);
    }
    this.keycloakClient = builder.build();
  }

  protected ResteasyClient buildResteasyClient() {
    final ResteasyClientBuilder clientBuilder =
        (ResteasyClientBuilder) ClientBuilder.newBuilder().connectTimeout(10, TimeUnit.SECONDS);
    configureSslForResteasyClient(clientBuilder);
    setProxyToResteasyClient(clientBuilder);

    clientBuilder.register(JacksonProvider.class, 100);

    ResteasyClient client = clientBuilder.build();
    LOG.info("Using resteasy client {}", client);
    return client;
  }

  protected void configureSslForResteasyClient(ResteasyClientBuilder resteasyClientBuilder) {
    if (authConfig.ignoreSsl()) {
      LOG.info("Ignoring ssl");
      try {
        TrustManager[] trustAllCerts =
            new TrustManager[] {
              new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                  return new java.security.cert.X509Certificate[0];
                }

                public void checkClientTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
                  // do nothing
                }

                public void checkServerTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
                  // do nothing
                }
              }
            };

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        resteasyClientBuilder.sslContext(sc);
      } catch (Exception ex) {
        throw new IllegalStateException("Unable to ignore ssl for resteasy client", ex);
      }
    } else if (StringUtils.isNotBlank(authConfig.trustStoreLocation())) {
      LOG.info("Using truststore '{}'", authConfig.trustStoreLocation());
      try {
        File f = new File(authConfig.trustStoreLocation());
        KeyStore trustStore =
            KeyStore.getInstance(f, authConfig.trustStorePassword().toCharArray());
        resteasyClientBuilder.trustStore(trustStore);
      } catch (KeyStoreException
          | IOException
          | NoSuchAlgorithmException
          | CertificateException ex) {
        throw new IllegalStateException("Unable to configure truststore for resteasy client", ex);
      }
    }
  }

  protected void setProxyToResteasyClient(ResteasyClientBuilder resteasyClientBuilder) {
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

  @Override
  public <R, A> ClientCall<R, A> interceptCall(
      MethodDescriptor<R, A> method, CallOptions callOptions, Channel next) {

    return new ForwardingClientCall.SimpleForwardingClientCall<>(
        next.newCall(method, callOptions)) {

      @Override
      public void start(Listener<A> responseListener, Metadata headers) {
        try {
          String accessToken = keycloakClient.tokenManager().getAccessTokenString();
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
        }
      }
    };
  }
}
