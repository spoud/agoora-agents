package io.spoud.agoora.agents.mqtt.config.data;

import java.time.Duration;

public interface ScrapperConfig {
  Integer maxSamples();
  Duration period();
  Duration maxWait();
  Duration waitTimeBeforeCountingRetained();
  ScrapperFeatureConfig profiling();
  ScrapperFeatureConfig hooks();
}
