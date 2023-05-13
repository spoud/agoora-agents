package io.spoud.agoora.agents.api.quarkus;

import io.quarkus.arc.DefaultBean;
import io.spoud.agoora.agents.api.config.AgooraAgentClientAuthConfig;
import io.spoud.agoora.agents.api.config.AgooraAgentConfig;
import io.spoud.agoora.agents.api.config.AgooraAgentEndpointConfig;
import io.spoud.agoora.agents.api.config.AgooraTransportConfig;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.ws.rs.Produces;

@Dependent
public class DefaultConfiguration {

  @Produces
  @Default
  @DefaultBean
  public AgooraAgentConfig agooraAgentConfig() {
    return new AgooraAgentConfig() {
      @Override
      public AgooraAgentClientAuthConfig auth() {
        return null;
      }

      @Override
      public AgooraTransportConfig transport() {
        return null;
      }

      @Override
      public AgooraAgentEndpointConfig logistics() {
        return null;
      }

      @Override
      public AgooraAgentEndpointConfig hooks() {
        return null;
      }

      @Override
      public AgooraAgentEndpointConfig schema() {
        return null;
      }

      @Override
      public AgooraAgentEndpointConfig looker() {
        return null;
      }

      @Override
      public AgooraAgentEndpointConfig blob() {
        return null;
      }

      @Override
      public AgooraAgentEndpointConfig profiler() {
        return null;
      }
    };
  }
}
