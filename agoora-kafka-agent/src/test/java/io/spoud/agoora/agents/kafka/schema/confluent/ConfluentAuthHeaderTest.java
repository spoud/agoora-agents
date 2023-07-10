package io.spoud.agoora.agents.kafka.schema.confluent;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.agoora.agents.kafka.config.data.RegistryConfig;
import io.spoud.agoora.agents.kafka.config.data.RegistryConfluentConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

@QuarkusTest
class ConfluentAuthHeaderTest {

  KafkaAgentConfig config;
  RegistryConfig registry;
  RegistryConfluentConfig confluent;

  @BeforeEach
  void setup() {
    config = mock(KafkaAgentConfig.class);
    registry = mock(RegistryConfig.class);
    Mockito.when(config.registry()).thenReturn(registry);
    confluent = mock(RegistryConfluentConfig.class);
    Mockito.when(registry.confluent()).thenReturn(confluent);
  }

  @Test
  void testHeaderWithAuth() {
    Mockito.when(confluent.apiKey()).thenReturn(Optional.of("apiKey"));
    Mockito.when(confluent.apiSecret()).thenReturn(Optional.of("apiSecret"));

    final ConfluentAuthHeader confluentAuthHeader =
        new ConfluentAuthHeader(config);

    final MultivaluedMap<String, String> result =
        confluentAuthHeader.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());
    assertThat(result)
        .containsEntry("Authorization", Arrays.asList("Basic YXBpS2V5OmFwaVNlY3JldA=="));
  }

  @Test
  void testHeaderWithoutAuth() {
    Mockito.when(confluent.apiKey()).thenReturn(Optional.empty());
    Mockito.when(confluent.apiSecret()).thenReturn(Optional.empty());


    final ConfluentAuthHeader confluentAuthHeader =
        new ConfluentAuthHeader(config);

    final MultivaluedMap<String, String> result =
        confluentAuthHeader.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());
    assertThat(result).isEmpty();
  }
}
