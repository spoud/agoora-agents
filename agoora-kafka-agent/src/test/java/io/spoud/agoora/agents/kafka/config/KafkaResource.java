package io.spoud.agoora.agents.kafka.config;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

public class KafkaResource implements QuarkusTestResourceLifecycleManager {

  public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0")).withNetwork(NetworkConfig.NETWORK).withKraft() ;

  @Override
  public Map<String, String> start() {
    kafka.start();
    return Map.of("agoora.kafka.bootstrap-servers", kafka.getBootstrapServers());
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
