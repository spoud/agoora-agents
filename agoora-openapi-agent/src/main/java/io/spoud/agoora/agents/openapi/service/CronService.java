package io.spoud.agoora.agents.openapi.service;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.spoud.agoora.agents.api.metrics.OperationalMetricsService;
import io.spoud.agoora.agents.openapi.config.data.OpenApiAgooraConfig;
import io.spoud.agoora.agents.openapi.config.data.ScrapperFeatureConfig;
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

  private final ManagedExecutor managedExecutor;

  private final OpenApiAgooraConfig config;

    private final OperationalMetricsService operationalMetricsService;

  void onStart(@Observes StartupEvent ev) {
    ScrapperFeatureConfig stateConfig = config.getScrapper().getState();

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
                    operationalMetricsService.iterationStart();
                  dataService.updateStates();

                    operationalMetricsService.iterationEnd(
                            config.getAuth().getUser().getName(),
                            config.getTransport().getAgooraPath(),
                            stateConfig.getInterval());
                } catch (Exception ex) {
                  LOG.error("Error while updating the data ports", ex);
                }
              });
    }
  }
}
