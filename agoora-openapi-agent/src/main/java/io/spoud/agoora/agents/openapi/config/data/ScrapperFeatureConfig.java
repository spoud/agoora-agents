package io.spoud.agoora.agents.openapi.config.data;

import lombok.Data;

import java.time.Duration;

public interface ScrapperFeatureConfig {
  boolean enabled();
  Duration initialDelay();
  default Duration interval() {return Duration.ofMinutes(15);}
}
