package io.spoud.agoora.agents.pgsql.service;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.spoud.agoora.agents.api.metrics.OperationalMetricsService;
import io.spoud.agoora.agents.pgsql.config.data.PgsqlAgooraConfig;
import io.spoud.agoora.agents.pgsql.config.data.ScrapperConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.context.ManagedExecutor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class CronService {

  private final DataService dataService;

  private final ProfilerService profilerService;

  private final ManagedExecutor managedExecutor;

  private final PgsqlAgooraConfig config;

  private final OperationalMetricsService operationalMetricsService;

  void onStart(@Observes StartupEvent ev) {
    final ScrapperConfig scrapperConfig = config.scrapper();

    Multi.createFrom()
        .ticks()
        .startingAfter(scrapperConfig.initialDelay())
        .every(scrapperConfig.interval())
        .runSubscriptionOn(managedExecutor)
        .subscribe()
        .with(
            v -> {
              try {
                operationalMetricsService.iterationStart();

                if (scrapperConfig.state().enabled()) {
                  LOG.info("Start looking at data ports");
                  dataService.updateStates();
                }

                if (scrapperConfig.profiling().enabled()) {
                  LOG.info("Start Profiling");
                  profilerService.runProfiler();
                }

                operationalMetricsService.iterationEnd(
                    config.auth().user().name(),
                    config.transport().agooraPath(),
                    scrapperConfig.interval());
              } catch (Exception ex) {
                LOG.error("Error while updating the data ports", ex);
              }
            });
  }
}
