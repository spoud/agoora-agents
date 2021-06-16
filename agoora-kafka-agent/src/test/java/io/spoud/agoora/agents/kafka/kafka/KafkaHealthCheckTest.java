package io.spoud.agoora.agents.kafka.kafka;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.kafka.AbstractService;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.inject.Inject;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class KafkaHealthCheckTest extends AbstractService {

  @Inject KafkaAgentConfig config;

  @Test
  @Timeout(10)
  void testNormalCase() {
    KafkaHealthCheck kafkaHealthCheck = new KafkaHealthCheck(config);
    kafkaHealthCheck.postConstruct();

    final HealthCheckResponse call = kafkaHealthCheck.call();
    assertThat(call.getState()).isEqualTo(HealthCheckResponse.State.UP);
    assertThat(call.getName()).isEqualTo("Kafka connection health check");
    assertThat(call.getData()).isPresent();
    assertThat(call.getData().get())
        .isEqualTo(
            Map.of("nodes", config.getKafka().getBootstrapServers().replace("PLAINTEXT://", "")));
  }

  @Test
  @Disabled // disabled because it takes more than 1 min to complete
  @Timeout(120)
  void testNoConnection() {
    final String backup = config.getKafka().getBootstrapServers();
    config.getKafka().setBootstrapServers("localhost:12345");
    KafkaHealthCheck kafkaHealthCheck = new KafkaHealthCheck(config);
    kafkaHealthCheck.postConstruct();
    config.getKafka().setBootstrapServers(backup); // restore config after the post construct

    final HealthCheckResponse call = kafkaHealthCheck.call();
    assertThat(call.getState()).isEqualTo(HealthCheckResponse.State.DOWN);
    assertThat(call.getName()).isEqualTo("Kafka connection health check");
    assertThat(call.getData()).isPresent();
    assertThat((String) call.getData().get().get("reason"))
        .contains("org.apache.kafka.common.errors.TimeoutException: Call(callName=listNodes");
  }
}
