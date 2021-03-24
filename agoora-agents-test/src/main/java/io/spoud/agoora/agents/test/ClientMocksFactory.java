package io.spoud.agoora.agents.test;

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
import io.spoud.agoora.agents.api.factory.ClientsFactory;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientMocksFactory implements ClientsFactory {

  private Map<Class<?>, Object> singleton = new ConcurrentHashMap<>();

  @Override
  public void close() {
    // nothing to do
  }

  @Override
  public BlobClient getBlobClient() {
    return getSingleton(BlobClient.class);
  }

  @Override
  public DataPortClient getDataPortClient() {
    return getSingleton(DataPortClient.class);
  }

  @Override
  public DataItemClient getDataItemClient() {
    return getSingleton(DataItemClient.class);
  }

  @Override
  public DataSubscriptionStateClient getDataSubscriptionStateClient() {
    return getSingleton(DataSubscriptionStateClient.class);
  }

  @Override
  public TransportClient getTransportClient() {
    return getSingleton(TransportClient.class);
  }

  @Override
  public ResourceGroupClient getResourceGroupClient() {
    return getSingleton(ResourceGroupClient.class);
  }

  @Override
  public HooksClient getHooksClient() {
    return getSingleton(HooksClient.class);
  }

  @Override
  public LookerClient getLookerClient() {
    return getSingleton(LookerClient.class);
  }

  @Override
  public MetricsClient getMetricsClient() {
    return getSingleton(MetricsClient.class);
  }

  @Override
  public ProfilerClient getProfilerClient() {
    return getSingleton(ProfilerClient.class);
  }

  @Override
  public SchemaClient getSchemaClient() {
    return getSingleton(SchemaClient.class);
  }

  private <T> T getSingleton(Class<T> clazz) {
    return (T) singleton.computeIfAbsent(clazz, Mockito::mock);
  }
}
