package io.spoud.agoora.agents.api.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"token>"})
public class AgooraAgentUserConfig {

  private String name;

  private String token;
}
