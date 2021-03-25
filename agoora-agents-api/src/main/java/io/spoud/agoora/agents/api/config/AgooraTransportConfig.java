package io.spoud.agoora.agents.api.config;

import io.spoud.agoora.agents.api.utils.AgooraPath;
import lombok.Data;

@Data
public class AgooraTransportConfig {

  private String agooraPath;

  public AgooraPath getAgooraPathObject() {
    return AgooraPath.parse(agooraPath);
  }
}
