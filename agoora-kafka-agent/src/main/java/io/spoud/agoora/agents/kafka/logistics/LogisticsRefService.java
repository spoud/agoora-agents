package io.spoud.agoora.agents.kafka.logistics;

import io.spoud.agoora.agents.api.client.TransportClient;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.sdm.global.selection.v1.IdPathRef;
import io.spoud.sdm.logistics.domain.v1.Transport;
import lombok.RequiredArgsConstructor;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
@RequiredArgsConstructor
public class LogisticsRefService {

  private final KafkaAgentConfig config;
  private final TransportClient transportClient;

  private AtomicReference<IdPathRef> transportRef = new AtomicReference<>(null);
  private AtomicReference<String> transportId = new AtomicReference<>(null);
  private AtomicReference<IdPathRef> resourceRef = new AtomicReference<>(null);

  public IdPathRef getTransportRef() {
    return transportRef.updateAndGet(
        ref -> {
          if (ref == null) {
            ref =
                IdPathRef.newBuilder()
                    .setPath(config.transport().getAgooraPathObject().getAbsolutePath())
                    .build();
          }
          return ref;
        });
  }

  /**
   * Id of the transport this agent manages, resolved once from its path. Throws if logistics
   * cannot be reached or does not know the transport yet.
   */
  public String getTransportId() {
    return transportId.updateAndGet(
        id -> {
          if (id == null) {
            id =
                Optional.ofNullable(transportClient.getTransport(getTransportRef()))
                    .map(Transport::getId)
                    .filter(s -> !s.isEmpty())
                    .orElseThrow(
                        () -> new IllegalStateException("Transport " + getTransportRef() + " not found"));
          }
          return id;
        });
  }

  public IdPathRef getResourceGroupRef() {
    return resourceRef.updateAndGet(
        ref -> {
          if (ref == null) {
            ref =
                IdPathRef.newBuilder()
                    .setPath(config.transport().getAgooraPathObject().getResourceGroupPath())
                    .build();
          }
          return ref;
        });
  }
}
