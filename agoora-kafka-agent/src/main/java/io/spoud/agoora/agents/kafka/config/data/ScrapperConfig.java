package io.spoud.agoora.agents.kafka.config.data;

import java.time.Duration;

public interface ScrapperConfig {
  Integer maxSamples();
  Duration period();
  ScrapperFeatureConfig profiling();
  ScrapperFeatureConfig hooks();
}
