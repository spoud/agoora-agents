package io.spoud.agoora.agents.kafka.service;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.agoora.agents.kafka.config.data.ScrapperConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class CronService {

  private final DataService dataService;

  private final ExecutorService managedExecutor = Executors.newSingleThreadExecutor();

  private final KafkaAgentConfig sdmConfig;

  void onStart(@Observes StartupEvent ev) {
    if (LaunchMode.current() != LaunchMode.TEST) {
      final ScrapperConfig scrapperConfig = sdmConfig.getScrapper();

      if (scrapperConfig.getPeriod().compareTo(scrapperConfig.getMaxWait()) < 0) {
        LOG.error("Max wait should be smaller than than the period");
      } else {
        startCron(scrapperConfig);
      }
    }
  }

  private void startCron(ScrapperConfig scrapperConfig) {
    LOG.info("Staring cron with a period of {}", scrapperConfig.getPeriod());
    AtomicBoolean running = new AtomicBoolean(true);

    Multi.createFrom()
        .ticks()
        .every(scrapperConfig.getPeriod())
        .runSubscriptionOn(managedExecutor)
        .subscribe()
        .with(
            unused -> {
              if (!running.getAndSet(true)) {
                LOG.info("Scrapping topics");
                dataService.updateTopics();
                LOG.info("Scrapping consumer groups");
                dataService.updateConsumerGroups();

                if (scrapperConfig.getProfiling().isEnabled()) {
                  LOG.info("Profiling data");
                  dataService.profileData();
                }
                running.set(false);
              } else {
                LOG.error("Previous iteration was not finished, skipping this one");
              }
            });
  }
}
