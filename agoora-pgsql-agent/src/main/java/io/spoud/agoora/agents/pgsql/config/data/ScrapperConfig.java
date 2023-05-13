package io.spoud.agoora.agents.pgsql.config.data;

import java.time.Duration;

public interface ScrapperConfig {
  Integer samplesSize();
 Duration initialDelay();
  Duration interval();
 ScrapperFeatureConfig state();
  ScrapperFeatureConfig profiling();
  ScrapperFeatureConfig hooks();
}
