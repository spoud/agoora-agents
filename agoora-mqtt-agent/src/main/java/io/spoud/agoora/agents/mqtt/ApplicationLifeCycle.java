package io.spoud.agoora.agents.mqtt;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ConfigUtils;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@Slf4j
@ApplicationScoped
public class ApplicationLifeCycle {

  void onStart(@Observes StartupEvent ev) {
      LOG.info("The application is starting with profile {}", ConfigUtils.getProfiles());
  }
}
