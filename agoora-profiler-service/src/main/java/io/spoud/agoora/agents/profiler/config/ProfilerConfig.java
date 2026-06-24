package io.spoud.agoora.agents.profiler.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "profiler")
public interface ProfilerConfig {

    @WithDefault("30")
    @WithName("timeout")
    int timeout();

    @WithDefault("default")
    @WithName("service-version")
    String serviceVersion();
}
