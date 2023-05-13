package io.spoud.agoora.agents.pgsql.service;

import io.spoud.agoora.agents.pgsql.config.data.PgsqlAgooraConfig;
import io.spoud.sdm.global.selection.v1.BaseRef;
import io.spoud.sdm.global.selection.v1.IdPathRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class ReferenceService {

  private final PgsqlAgooraConfig config;

  @ConfigProperty(name = "quarkus.datasource.jdbc.url")
  String jdbcUrl;

  public BaseRef getTransportRef() {
    return BaseRef.newBuilder()
        .setIdPath(
            IdPathRef.newBuilder()
                .setPath(config.transport().getAgooraPathObject().getAbsolutePath())
                .build())
        .build();
  }

  public String getDatabaseUrl() {
    return jdbcUrl;
  }
}
