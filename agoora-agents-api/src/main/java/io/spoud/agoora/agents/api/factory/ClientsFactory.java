package io.spoud.agoora.agents.api.factory;

import io.spoud.agoora.agents.api.client.BlobClient;
import io.spoud.agoora.agents.api.client.DataItemClient;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.agoora.agents.api.client.DataSubscriptionStateClient;
import io.spoud.agoora.agents.api.client.HooksClient;
import io.spoud.agoora.agents.api.client.LookerClient;
import io.spoud.agoora.agents.api.client.MetricsClient;
import io.spoud.agoora.agents.api.client.ProfilerClient;
import io.spoud.agoora.agents.api.client.ResourceGroupClient;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.api.client.TransportClient;

public interface ClientsFactory {

  void closeAll();

  BlobClient getBlobClient();

  DataPortClient getDataPortClient();

  DataItemClient getDataItemClient();

  DataSubscriptionStateClient getDataSubscriptionStateClient();

  TransportClient getTransportClient();

  HooksClient getHooksClient();

  LookerClient getLookerClient();

  MetricsClient getMetricsClient();

  ProfilerClient getProfilerClient();

  SchemaClient getSchemaClient();

  ResourceGroupClient getResourceGroupClient();
}
