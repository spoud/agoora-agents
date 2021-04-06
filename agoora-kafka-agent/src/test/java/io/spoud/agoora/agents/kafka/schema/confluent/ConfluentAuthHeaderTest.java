package io.spoud.agoora.agents.kafka.schema.confluent;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.agoora.agents.kafka.config.data.RegistryConfig;
import io.spoud.agoora.agents.kafka.config.data.RegistryConfluentConfig;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Java6Assertions.assertThat;

@QuarkusTest
class ConfluentAuthHeaderTest {
  @Test
  void testHeaderWithAuth() {
    final ConfluentAuthHeader confluentAuthHeader =
        new ConfluentAuthHeader(
            KafkaAgentConfig.builder()
                .registry(
                    RegistryConfig.builder()
                        .confluent(
                            RegistryConfluentConfig.builder()
                                .apiKey(Optional.of("apiKey"))
                                .apiSecret(Optional.of("apiSecret"))
                                .build())
                        .build())
                .build());

    final MultivaluedMap<String, String> result =
        confluentAuthHeader.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());
    assertThat(result)
        .containsEntry("Authorization", Arrays.asList("Basic YXBpS2V5OmFwaVNlY3JldA=="));
  }

  @Test
  void testHeaderWithoutAuth() {
    final ConfluentAuthHeader confluentAuthHeader =
        new ConfluentAuthHeader(
            KafkaAgentConfig.builder()
                .registry(
                    RegistryConfig.builder()
                        .confluent(
                            RegistryConfluentConfig.builder()
                                .apiKey(Optional.empty())
                                .apiSecret(Optional.empty())
                                .build())
                        .build())
                .build());

    final MultivaluedMap<String, String> result =
        confluentAuthHeader.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());
    assertThat(result).isEmpty();
  }
}
