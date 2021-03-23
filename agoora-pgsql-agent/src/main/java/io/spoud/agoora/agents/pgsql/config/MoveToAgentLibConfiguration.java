package io.spoud.agoora.agents.pgsql.config;

import io.quarkus.arc.profile.UnlessBuildProfile;
import io.spoud.agoora.agents.api.client.BlobClient;
import io.spoud.agoora.agents.api.client.DataItemClient;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.agoora.agents.api.client.HooksClient;
import io.spoud.agoora.agents.api.client.LookerClient;
import io.spoud.agoora.agents.api.client.MetricsClient;
import io.spoud.agoora.agents.api.client.ProfilerClient;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.api.factory.ClientsFactory;
import io.spoud.agoora.agents.api.factory.ClientsFactoryImpl;
import io.spoud.agoora.agents.pgsql.config.data.PgsqlSdmConfig;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

@Slf4j
@Dependent
@UnlessBuildProfile("test")
public class MoveToAgentLibConfiguration {

  private final ClientsFactory clientsFactory;

  public MoveToAgentLibConfiguration(PgsqlSdmConfig pgsqlSdmConfig) {
    clientsFactory = new ClientsFactoryImpl(pgsqlSdmConfig);
  }

  @Produces
  BlobClient blobClient() {
    return clientsFactory.getBlobClient();
  }

  @Produces
  DataPortClient dataPortClient() {
    return clientsFactory.getDataPortClient();
  }

  @Produces
  DataItemClient dataItemClient() {
    return clientsFactory.getDataItemClient();
  }

  @Produces
  HooksClient hooksClient() {
    return clientsFactory.getHooksClient();
  }

  @Produces
  LookerClient lookerClient() {
    return clientsFactory.getLookerClient();
  }

  @Produces
  MetricsClient metricsClient() {
    return clientsFactory.getMetricsClient();
  }

  @Produces
  ProfilerClient profilerClient() {
    return clientsFactory.getProfilerClient();
  }

  @Produces
  SchemaClient schemaClient() {
    return clientsFactory.getSchemaClient();
  }
}