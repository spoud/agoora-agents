package io.spoud.agoora.agents.api.factory;

import io.spoud.agoora.agents.api.config.AgooraAgentConfig;
import io.spoud.agoora.agents.api.utils.LazySingletonInstance;
import io.spoud.sdm.logistics.service.v1.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogisticsClientsFactory extends AbstractGrpcClientFactory {

  private final LazySingletonInstance<DataPortServiceGrpc.DataPortServiceBlockingStub>
      dataPortServiceStub;
  private final LazySingletonInstance<DataItemServiceGrpc.DataItemServiceBlockingStub>
      dataItemServiceStub;
  private final LazySingletonInstance<
          DataSubscriptionStateServiceGrpc.DataSubscriptionStateServiceBlockingStub>
      dataSubscriptionStateServiceStub;
  private final LazySingletonInstance<TransportServiceGrpc.TransportServiceBlockingStub>
      transportServiceStub;
  private final LazySingletonInstance<ResourceGroupServiceGrpc.ResourceGroupServiceBlockingStub>
      resourceGroupStub;

  public LogisticsClientsFactory(AgooraAgentConfig config) {
    super(LOG, config.logistics(), config.auth());
    dataPortServiceStub =
        new LazySingletonInstance<>(
            () -> DataPortServiceGrpc.newBlockingStub(channel.getInstance()));
    dataItemServiceStub =
        new LazySingletonInstance<>(
            () -> DataItemServiceGrpc.newBlockingStub(channel.getInstance()));
    dataSubscriptionStateServiceStub =
        new LazySingletonInstance<>(
            () -> DataSubscriptionStateServiceGrpc.newBlockingStub(channel.getInstance()));
    transportServiceStub =
        new LazySingletonInstance<>(
            () -> TransportServiceGrpc.newBlockingStub(channel.getInstance()));
    resourceGroupStub =
        new LazySingletonInstance<>(
            () -> ResourceGroupServiceGrpc.newBlockingStub(channel.getInstance()));
  }

  public DataPortServiceGrpc.DataPortServiceBlockingStub dataPortServiceStub() {
    return dataPortServiceStub.getInstance();
  }

  public DataItemServiceGrpc.DataItemServiceBlockingStub dataItemServiceStub() {
    return dataItemServiceStub.getInstance();
  }

  public DataSubscriptionStateServiceGrpc.DataSubscriptionStateServiceBlockingStub
      dataSubscriptionStateServiceStub() {
    return dataSubscriptionStateServiceStub.getInstance();
  }

  public TransportServiceGrpc.TransportServiceBlockingStub transportClient() {
    return transportServiceStub.getInstance();
  }

  public ResourceGroupServiceGrpc.ResourceGroupServiceBlockingStub resourceGroupClient() {
    return resourceGroupStub.getInstance();
  }
}
