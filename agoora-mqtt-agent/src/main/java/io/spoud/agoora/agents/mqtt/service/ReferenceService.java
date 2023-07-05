package io.spoud.agoora.agents.mqtt.service;

import io.spoud.agoora.agents.mqtt.config.data.MqttAgooraConfig;
import io.spoud.sdm.global.selection.v1.BaseRef;
import io.spoud.sdm.global.selection.v1.IdPathRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class ReferenceService {

  private final MqttAgooraConfig config;

  public BaseRef getTransportRef() {
    return BaseRef.newBuilder()
        .setIdPath(
            IdPathRef.newBuilder()
                .setPath(config.transport().getAgooraPathObject().getAbsolutePath())
                .build())
        .build();
  }
}
