package io.spoud.agoora.agents.api.quarkus;

import io.quarkus.arc.DefaultBean;
import io.spoud.agoora.agents.api.config.AgooraAgentConfig;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.ws.rs.Produces;

@Dependent
public class DefaultConfiguration {

  @Produces
  @Default
  @DefaultBean
  public AgooraAgentConfig agooraAgentConfig() {
    return AgooraAgentConfig.builder().build();
  }
}
