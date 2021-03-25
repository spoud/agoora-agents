package io.spoud.agoora.agents.pgsql.service;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.spoud.agoora.agents.pgsql.config.data.PgsqlAgooraConfig;
import io.spoud.agoora.agents.pgsql.config.data.ScrapperFeatureConfig;
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

  void onStart(@Observes StartupEvent ev) {
    ScrapperFeatureConfig stateConfig = config.getScrapper().getState();
    ScrapperFeatureConfig profilingConfig = config.getScrapper().getProfiling();

    if (stateConfig.isEnabled()) {
      Multi.createFrom()
          .ticks()
          .startingAfter(stateConfig.getInitialDelay())
          .every(stateConfig.getInterval())
          .runSubscriptionOn(managedExecutor)
          .subscribe()
          .with(
              v -> {
                LOG.info("Start looking at data ports");
                try {
                  dataService.updateStates();
                } catch (Exception ex) {
                  LOG.error("Error while updating the data ports", ex);
                }
              });
    }

    if (profilingConfig.isEnabled()) {
      Multi.createFrom()
          .ticks()
          .startingAfter(profilingConfig.getInitialDelay())
          .every(profilingConfig.getInterval())
          .runSubscriptionOn(managedExecutor)
          .subscribe()
          .with(
              v -> {
                LOG.info("Start Profiling");
                try {
                  profilerService.runProfiler();
                } catch (Exception ex) {
                  LOG.error("Error while profiling", ex);
                }
              });
    }
  }
}
