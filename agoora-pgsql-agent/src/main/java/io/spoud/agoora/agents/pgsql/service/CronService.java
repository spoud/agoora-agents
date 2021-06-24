package io.spoud.agoora.agents.pgsql.service;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.spoud.agoora.agents.api.metrics.OperationalMetricsService;
import io.spoud.agoora.agents.pgsql.config.data.PgsqlAgooraConfig;
import io.spoud.agoora.agents.pgsql.config.data.ScrapperConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.context.ManagedExecutor;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

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
    final ScrapperConfig scrapperConfig = config.getScrapper();

    Multi.createFrom()
        .ticks()
        .startingAfter(scrapperConfig.getInitialDelay())
        .every(scrapperConfig.getInterval())
        .runSubscriptionOn(managedExecutor)
        .subscribe()
        .with(
            v -> {
              try {
                operationalMetricsService.iterationStart();

                if (scrapperConfig.getState().isEnabled()) {
                  LOG.info("Start looking at data ports");
                  dataService.updateStates();
                }

                if (scrapperConfig.getProfiling().isEnabled()) {
                  LOG.info("Start Profiling");
                  try {
                    profilerService.runProfiler();
                  } catch (Exception ex) {
                    LOG.error("Error while profiling", ex);
                  }
                }

                operationalMetricsService.iterationEnd(
                    config.getAuth().getUser().getName(),
                    config.getTransport().getAgooraPath(),
                    scrapperConfig.getInterval());
              } catch (Exception ex) {
                LOG.error("Error while updating the data ports", ex);
              }
            });
  }
}
