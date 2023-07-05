package io.spoud.agoora.agents.pgsql;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ProfileManager;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@Slf4j
@ApplicationScoped
public class ApplicationLifeCycle {

  void onStart(@Observes StartupEvent ev) {
    LOG.info("The application is starting with profile " + ProfileManager.getActiveProfile());
  }
}
