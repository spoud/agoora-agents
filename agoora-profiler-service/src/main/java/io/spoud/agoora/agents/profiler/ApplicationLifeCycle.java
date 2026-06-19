package io.spoud.agoora.agents.profiler;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.spoud.agoora.agents.profiler.config.ProfilerConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ApplicationLifeCycle {

    private final ProfilerConfig config;

    void onStart(@Observes StartupEvent ev) {
        LOG.info(
                "Profiler service starting with profile {}. Version: {}, Timeout: {}s",
                ConfigUtils.getProfiles(),
                config.serviceVersion(),
                config.timeout());
    }
}
