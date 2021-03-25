package io.spoud.agoora.agents.mqtt.config.profile;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class HooksOnlyProfile implements QuarkusTestProfile {

  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of("sdm.scrapper.hooks.enabled", "true");
  }
}
