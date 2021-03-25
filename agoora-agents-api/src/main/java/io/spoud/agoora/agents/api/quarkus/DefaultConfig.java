package io.spoud.agoora.agents.api.quarkus;

import io.quarkus.arc.DefaultBean;
import io.spoud.agoora.agents.api.config.SdmAgentConfig;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.ws.rs.Produces;

@Dependent
public class DefaultConfig {

  @Produces
  @Default
  @DefaultBean
  public SdmAgentConfig sdmAgentConfig() {
    return SdmAgentConfig.builder().build();
  }
}