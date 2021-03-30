package io.spoud.agoora.agents.test;

import io.spoud.agoora.agents.api.client.BlobClient;
import io.spoud.agoora.agents.api.client.DataItemClient;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.agoora.agents.api.client.DataSubscriptionStateClient;
import io.spoud.agoora.agents.api.client.HooksClient;
import io.spoud.agoora.agents.api.client.LookerClient;
import io.spoud.agoora.agents.api.client.MetricsClient;
import io.spoud.agoora.agents.api.client.ProfilerClient;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.api.factory.ClientsFactory;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

@Slf4j
@Dependent
public class AgentLibConfiguration {

  private static final ClientsFactory clientsFactory = new ClientMocksFactory();

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
  DataSubscriptionStateClient dataSubscriptionStateClient() {
    return clientsFactory.getDataSubscriptionStateClient();
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
