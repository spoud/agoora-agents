package io.spoud.agoora.agents.api.auth.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetTokenResponse {

  @JsonProperty("access_token")
  public String accessToken;

  @JsonProperty("expires_in")
  public Long expiresIn;

  @JsonProperty("refresh_expires_in")
  public Long refreshExpiresIn;

  @JsonProperty("refresh_token")
  public String refreshToken;

  @JsonProperty("token_type")
  public String tokenType;

  @JsonProperty("not_before_policy")
  public Long notBeforePolicy;

  @JsonProperty("session_state")
  public String sessionState;

  @JsonProperty("scope")
  public String scope;
}
