package io.spoud.agoora.agents.kafka.schema.confluent;

import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.agoora.agents.kafka.config.data.RegistryConfluentConfig;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedMap;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@ApplicationScoped
public class ConfluentAuthHeader implements ClientHeadersFactory {

  private final Optional<String> authHeader;

  public ConfluentAuthHeader(KafkaAgentConfig kafkaAgentConfig) {
    final RegistryConfluentConfig config = kafkaAgentConfig.registry().confluent();

    authHeader =
        config
            .apiKey()
            .flatMap(
                apiKey ->
                    config
                        .apiSecret()
                        .map(
                            apiSecret -> {
                              String auth = apiKey + ":" + apiSecret;
                              byte[] encodedAuth =
                                  Base64.encodeBase64(auth.getBytes(StandardCharsets.US_ASCII));
                              return "Basic " + new String(encodedAuth);
                            }));
  }

  @Override
  public MultivaluedMap<String, String> update(
      MultivaluedMap<String, String> incomingHeaders,
      MultivaluedMap<String, String> clientOutgoingHeaders) {
    MultivaluedMap<String, String> result = new MultivaluedMapImpl<>();
    authHeader.ifPresent(value -> result.add("Authorization", value));
    return result;
  }
}
