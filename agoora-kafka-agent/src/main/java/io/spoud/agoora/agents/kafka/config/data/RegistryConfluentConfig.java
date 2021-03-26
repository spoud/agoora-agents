package io.spoud.agoora.agents.kafka.config.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistryConfluentConfig {

  private Optional<String> url;
  private Optional<String> apiKey;
  private Optional<String> apiSecret;
  private Optional<String> publicUrl;
}
