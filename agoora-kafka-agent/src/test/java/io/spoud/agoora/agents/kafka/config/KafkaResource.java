package io.spoud.agoora.agents.kafka.config;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.KafkaContainer;

import java.util.Map;

public class KafkaResource implements QuarkusTestResourceLifecycleManager {

  public static KafkaContainer kafka = new KafkaContainer().withNetwork(NetworkConfig.NETWORK);

  @Override
  public Map<String, String> start() {
    kafka.start();
    return Map.of("sdm.kafka.bootstrap-servers", kafka.getBootstrapServers());
  }

  @Override
  public void stop() {
    kafka.stop();
  }

  @Override
  public int order() {
    return 1;
  }
}
