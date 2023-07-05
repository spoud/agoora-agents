package io.spoud.agoora.agents.kafka;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ProfileManager;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ApplicationLifeCycle {

  private final KafkaAgentConfig config;

  void onStart(@Observes StartupEvent ev) {
    LOG.info(
        "The application is starting with profile {}.\n Config is \n {} ",
        ProfileManager.getActiveProfile(),
        config);
  }
}
