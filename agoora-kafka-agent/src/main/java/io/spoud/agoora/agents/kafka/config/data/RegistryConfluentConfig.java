package io.spoud.agoora.agents.kafka.config.data;

import java.util.Optional;

public interface RegistryConfluentConfig {

  Optional<String> url();
  Optional<String> apiKey();
  Optional<String> apiSecret();
  Optional<String> publicUrl();
}
