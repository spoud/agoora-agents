package io.spoud.agoora.agents.api.auth.token;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Builder
@RequiredArgsConstructor
@Data
public class JwtToken {
  private final String token;
  private final Instant expireInstant;
}
