package io.spoud.agoora.agents.kafka.config.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RegistryConfig {

  private RegistryConfluentConfig confluent;
}
