package io.spoud.agoora.agents.openapi.service;

import io.spoud.agoora.agents.openapi.config.data.OpenApiSdmConfig;
import io.spoud.sdm.global.selection.v1.BaseRef;
import io.spoud.sdm.global.selection.v1.IdPathRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class ReferenceService {

  private final OpenApiSdmConfig config;

  public BaseRef getTransportRef() {
    return BaseRef.newBuilder()
        .setIdPath(
            IdPathRef.newBuilder()
                .setPath(config.getTransport().getSdmPathObject().getAbsolutePath())
                .build())
        .build();
  }
}
