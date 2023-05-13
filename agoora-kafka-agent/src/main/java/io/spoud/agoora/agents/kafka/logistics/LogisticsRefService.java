package io.spoud.agoora.agents.kafka.logistics;

import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.sdm.global.selection.v1.IdPathRef;
import lombok.RequiredArgsConstructor;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
@RequiredArgsConstructor
public class LogisticsRefService {

  private final KafkaAgentConfig config;

  private AtomicReference<IdPathRef> transportRef = new AtomicReference<>(null);
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
