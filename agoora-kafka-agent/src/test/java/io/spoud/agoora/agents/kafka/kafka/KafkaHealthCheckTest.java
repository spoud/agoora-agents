package io.spoud.agoora.agents.kafka.kafka;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.kafka.AbstractService;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import jakarta.inject.Inject;

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
    assertThat(call.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    assertThat(call.getName()).isEqualTo("Kafka connection health check");
    assertThat(call.getData()).isPresent();
    assertThat(call.getData().get()).containsOnlyKeys("nodes");
    assertThat(call.getData().get().get("nodes"))
        .isEqualTo(config.kafka().bootstrapServers().replace("PLAINTEXT://", ""));

    kafkaHealthCheck.stop();
  }

  @Test
  @Disabled(" disabled because it takes more than 1 min to complete")
  @Timeout(120)
  void testNoConnection() {
    final String backup = config.kafka().bootstrapServers();
    Mockito.when(config.kafka().bootstrapServers()).thenReturn("localhost:12345");
    KafkaHealthCheck kafkaHealthCheck = new KafkaHealthCheck(config);
    kafkaHealthCheck.postConstruct();
    Mockito.when(config.kafka().bootstrapServers()).thenReturn(backup); // restore config after the post construct

    final HealthCheckResponse call = kafkaHealthCheck.call();
    assertThat(call.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
    assertThat(call.getName()).isEqualTo("Kafka connection health check");
    assertThat(call.getData()).isPresent();
    assertThat(call.getData().get()).containsOnlyKeys("reason");
    assertThat((String) call.getData().get().get("reason"))
        .contains("org.apache.kafka.common.errors.TimeoutException: Call(callName=listNodes");

    kafkaHealthCheck.stop();
  }
}
