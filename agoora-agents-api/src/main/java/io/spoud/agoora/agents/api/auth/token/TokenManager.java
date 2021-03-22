package io.spoud.agoora.agents.api.auth.token;

import io.spoud.agoora.agents.api.config.SdmAgentClientAuthConfig;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

// TODO better error handling
@Slf4j
public class TokenManager {
  public static final Duration JWT_TOKEN_EXPIRATION_MARGIN = Duration.ofSeconds(10);

  private final SdmAgentClientAuthConfig authConfig;
  private final ResteasyWebTarget target;
  private final String clientId;

  private JwtToken accessToken;
  private JwtToken refreshToken;

  public TokenManager(ResteasyClient client, SdmAgentClientAuthConfig authConfig, String clientId) {
    this.authConfig = authConfig;
    this.clientId = clientId;
    target = client.target(UriBuilder.fromPath(authConfig.getServerUrl()));
  }

  private synchronized String getRefreshToken() {
    if (refreshToken != null) {
      Instant now = Instant.now();
      if (refreshToken.getExpireInstant().isBefore(now)) {
        return refreshToken.getToken();
      }
    }

    final GetTokenResponse refreshTokenResponse = getRefreshTokenResponse();
    final Instant now = Instant.now();
    refreshToken =
        new JwtToken(
            refreshTokenResponse.getRefreshToken(),
            now.plus(Duration.ofSeconds(refreshTokenResponse.refreshExpiresIn))
                .minus(JWT_TOKEN_EXPIRATION_MARGIN));

    accessToken =
        new JwtToken(
            refreshTokenResponse.getAccessToken(),
            now.plus(Duration.ofSeconds(refreshTokenResponse.expiresIn))
                .minus(JWT_TOKEN_EXPIRATION_MARGIN));

    return refreshToken.getToken();
  }

  public synchronized String getAccessToken() {
    Instant now = Instant.now();
    if (accessToken != null) {
      if (accessToken.getExpireInstant().isBefore(now)) {
        return accessToken.getToken();
      }
    }

    final String refreshToken = getRefreshToken();
    // check the access token again because it comes with the refresh token
    if (accessToken.getExpireInstant().isBefore(now)) {
      return accessToken.getToken();
    }

    final GetTokenResponse accessTokenResponse = getAccessTokenResponse(refreshToken);

    accessToken =
        new JwtToken(
            accessTokenResponse.getAccessToken(),
            Instant.now()
                .plus(Duration.ofSeconds(accessTokenResponse.expiresIn))
                .minus(JWT_TOKEN_EXPIRATION_MARGIN));
    return accessToken.getToken();
  }

  private GetTokenResponse getRefreshTokenResponse() {
    return getTokenReponse(
        "password",
        Map.of(
            "username",
            authConfig.getUser().getName(),
            "password",
            authConfig.getUser().getToken()));
  }

  private GetTokenResponse getAccessTokenResponse(String refreshToken) {
    return getTokenReponse("refresh_token", Map.of("refresh_token", refreshToken));
  }

  private GetTokenResponse getTokenReponse(String grantType, Map<String, String> params) {
    MultivaluedMap map = new MultivaluedHashMap(params);
    map.add("client_id", clientId);
    map.add("grant_type", grantType);

    Entity<Form> postEntity = Entity.form(map);

    final GetTokenResponse response =
        target
            .clone()
            .path(getTokenPath(authConfig.getRealm()))
            .request(MediaType.APPLICATION_JSON)
            .buildPost(postEntity)
            .invoke(GetTokenResponse.class);
    return response;
  }

  private String getTokenPath(String realm) {
    return "realms/" + realm + "/protocol/openid-connect/token";
  }
}
