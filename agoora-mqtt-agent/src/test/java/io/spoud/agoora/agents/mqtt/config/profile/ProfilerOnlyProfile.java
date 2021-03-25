package io.spoud.agoora.agents.mqtt.config.profile;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class ProfilerOnlyProfile implements QuarkusTestProfile {

  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of("sdm.scrapper.profiling.enabled", "true");
  }
}
