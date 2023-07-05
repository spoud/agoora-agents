package io.spoud.agoora.agents.openapi.service;

import io.spoud.agoora.agents.openapi.config.data.OpenApiAgooraConfig;
import io.spoud.sdm.global.selection.v1.BaseRef;
import io.spoud.sdm.global.selection.v1.IdPathRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class ReferenceService {

  private final OpenApiAgooraConfig config;

  public BaseRef getTransportRef() {
    return BaseRef.newBuilder()
        .setIdPath(
            IdPathRef.newBuilder()
                .setPath(config.transport().getAgooraPathObject().getAbsolutePath())
                .build())
        .build();
  }
}
