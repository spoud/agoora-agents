package io.spoud.agoora.agents.api.config;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SdmAgentUserConfigImpl implements SdmAgentUserConfig {

  private String name;

  private String token;
}
