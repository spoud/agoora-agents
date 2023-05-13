package io.spoud.agoora.agents.api.config;

import io.spoud.agoora.agents.api.utils.AgooraPath;

public interface AgooraTransportConfig {

  String agooraPath();

  default AgooraPath getAgooraPathObject() {
    return AgooraPath.parse(agooraPath());
  }
}
