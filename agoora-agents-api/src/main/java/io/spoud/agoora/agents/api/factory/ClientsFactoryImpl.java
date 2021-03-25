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
import io.spoud.agoora.agents.api.config.AgooraAgentConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ClientsFactoryImpl implements ClientsFactory {

  private final BlobClientFactory blobClientFactory;
  private final HooksClientFactory hooksClientFactory;
  private final LogisticsClientsFactory logisticsClientsFactory;
  private final LookerFactory lookerFactory;
  private final ProfilerFactory profilerFactory;
  private final SchemaFactory schemaFactory;

  public ClientsFactoryImpl(AgooraAgentConfig config) {
    blobClientFactory = new BlobClientFactory(config);
    hooksClientFactory = new HooksClientFactory(config);
    logisticsClientsFactory = new LogisticsClientsFactory(config);
    lookerFactory = new LookerFactory(config);
    profilerFactory = new ProfilerFactory(config);
    schemaFactory = new SchemaFactory(config);
  }

  @Override
  public void close() throws Exception {
    blobClientFactory.close();
    hooksClientFactory.close();
    logisticsClientsFactory.close();
    lookerFactory.close();
    profilerFactory.close();
    schemaFactory.close();
  }

  @Override
  public BlobClient getBlobClient() {
    return new BlobClient(blobClientFactory.blobServiceStub());
  }

  @Override
  public DataPortClient getDataPortClient() {
    return new DataPortClient(logisticsClientsFactory.dataPortServiceStub());
  }

  @Override
  public DataItemClient getDataItemClient() {
    return new DataItemClient(logisticsClientsFactory.dataItemServiceStub());
  }

  @Override
  public DataSubscriptionStateClient getDataSubscriptionStateClient() {
    return new DataSubscriptionStateClient(
        logisticsClientsFactory.dataSubscriptionStateServiceStub());
  }

  @Override
  public TransportClient getTransportClient() {
    return new TransportClient(logisticsClientsFactory.transportClient());
  }

  @Override
  public ResourceGroupClient getResourceGroupClient() {
    return new ResourceGroupClient(logisticsClientsFactory.resourceGroupClient());
  }

  @Override
  public HooksClient getHooksClient() {
    return new HooksClient(hooksClientFactory.stateServiceStub());
  }

  @Override
  public LookerClient getLookerClient() {
    return new LookerClient(lookerFactory.lookerServiceStub());
  }

  @Override
  public MetricsClient getMetricsClient() {
    return new MetricsClient(lookerFactory.metricsServiceStub());
  }

  @Override
  public ProfilerClient getProfilerClient() {
    return new ProfilerClient(profilerFactory.profilerServiceStub());
  }

  @Override
  public SchemaClient getSchemaClient() {
    return new SchemaClient(schemaFactory.schemaServiceStub());
  }
}
