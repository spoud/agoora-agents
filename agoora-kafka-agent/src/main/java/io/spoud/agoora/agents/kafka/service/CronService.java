package io.spoud.agoora.agents.kafka.service;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.spoud.agoora.agents.api.metrics.OperationalMetricsService;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.agoora.agents.kafka.config.data.ScrapperConfig;
import io.spoud.agoora.agents.kafka.metrics.MetricsForwarderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class CronService {

  private final DataService dataService;
  private final ProfilerService profilerService;
  private final MetricsForwarderService metricsForwarderService;
  private final OperationalMetricsService operationalMetricsService;

  private final ExecutorService managedExecutor = Executors.newSingleThreadExecutor();

  private final KafkaAgentConfig sdmConfig;

  void onStart(@Observes StartupEvent ev) {
    if (LaunchMode.current() != LaunchMode.TEST) {
      final ScrapperConfig scrapperConfig = sdmConfig.scrapper();

      startCron(scrapperConfig);
    }
  }

  private void startCron(ScrapperConfig scrapperConfig) {
    LOG.info("Staring cron with a period of {}", scrapperConfig.period());
    AtomicBoolean running = new AtomicBoolean(false);

    Multi.createFrom()
        .ticks()
        .startingAfter(Duration.ofSeconds(5)) // wait for the complete initi
        .every(scrapperConfig.period())
        .runSubscriptionOn(managedExecutor)
        .subscribe()
        .with(
            unused -> {
              if (!running.getAndSet(true)) {
                try {
                  operationalMetricsService.iterationStart();

                  LOG.info("Scrapping topics");
                  dataService.updateTopics();

                  LOG.info("Scrapping consumer groups");
                  dataService.updateConsumerGroups();

                  LOG.info("Forwarding metrics");
                  metricsForwarderService.scrapeMetrics();

                  if (scrapperConfig.profiling().enabled()) {
                    LOG.info("Profiling data");
                    profilerService.profileData();
                  }

                  operationalMetricsService.iterationEnd(
                      sdmConfig.auth().user().name(),
                      sdmConfig.transport().agooraPath(),
                      scrapperConfig.period());

                } catch (Exception ex) {
                  LOG.error("Error while processing", ex);
                } finally {
                  running.set(false);
                }
              } else {
                LOG.error("Previous iteration was not finished, skipping this one");
              }
            });
  }
}
