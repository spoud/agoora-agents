package io.spoud.agoora.agents.mqtt.service;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.spoud.agoora.agents.api.metrics.OperationalMetricsService;
import io.spoud.agoora.agents.mqtt.config.data.MqttAgooraConfig;
import io.spoud.agoora.agents.mqtt.config.data.ScrapperConfig;
import io.spoud.agoora.agents.mqtt.mqtt.IterationContext;
import io.spoud.agoora.agents.mqtt.mqtt.MqttScrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class CronService {

  private final MqttScrapper mqttScrapper;

  private final ExecutorService managedExecutor = Executors.newSingleThreadExecutor();
  private final ScheduledExecutorService scheduledExecutorService =
      Executors.newSingleThreadScheduledExecutor();
  private final OperationalMetricsService operationalMetricsService;

  private final MqttAgooraConfig config;
  private IterationContext lastIterationContext;

  void onStart(@Observes StartupEvent ev) {
    if (LaunchMode.current() != LaunchMode.TEST) {
      final ScrapperConfig scrapperConfig = config.scrapper();

      if (scrapperConfig.period().compareTo(scrapperConfig.maxWait()) < 0) {
        LOG.error("Max wait should be smaller than than the period");
      } else {
        startCron(scrapperConfig);
      }
    }
  }

  private void startCron(ScrapperConfig scrapperConfig) {
    LOG.info("Staring cron with a period of {}", scrapperConfig.period());
    AtomicReference<ScheduledFuture<?>> terminationSchedule = new AtomicReference<>(null);

    Multi.createFrom()
        .ticks()
        .every(scrapperConfig.period())
        .runSubscriptionOn(managedExecutor)
        .subscribe()
        .with(
            unused -> {
              operationalMetricsService.iterationStart();
              final ScheduledFuture<?> old =
                  terminationSchedule.getAndSet(
                      scheduledExecutorService.schedule(
                          () -> stopScrapper(scrapperConfig),
                          scrapperConfig.maxWait().toMillis(),
                          TimeUnit.MILLISECONDS));
              // stop the previous iteration if not already executed
              if (old != null) {
                old.cancel(true);
                // stop previous iteration if needed
                stopScrapper(scrapperConfig);
              }

              LOG.info(
                  "Starting MQTT listening iteration, max-wait={}", scrapperConfig.maxWait());
              lastIterationContext = mqttScrapper.startIteration();
            });
  }

  private void stopScrapper(ScrapperConfig scrapperConfig) {
    mqttScrapper.stopRemainingOfPreviousIteration(lastIterationContext);

    operationalMetricsService.iterationEnd(
        config.auth().user().name(),
        config.transport().agooraPath(),
        scrapperConfig.period());
  }
}
