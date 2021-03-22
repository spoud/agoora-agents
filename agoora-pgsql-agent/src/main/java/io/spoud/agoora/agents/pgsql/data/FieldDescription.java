package io.spoud.agoora.agents.pgsql.data;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@RegisterForReflection
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDescription {
  private String name;
  private String type; // TODO enum ?
  private boolean nullable;
}
