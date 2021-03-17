package io.spoud.agoora.agents.api.factory;

import io.spoud.agoora.agents.api.config.SdmAgentConfig;
import io.spoud.agoora.agents.api.utils.LazySingletonInstance;
import io.spoud.sdm.looker.v1alpha1.LookerServiceGrpc;
import io.spoud.sdm.looker.v1alpha1.MetricsServiceGrpc;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LookerFactory extends AbstractGrpcClientFactory {

  private final LazySingletonInstance<MetricsServiceGrpc.MetricsServiceBlockingStub>
      metricServiceStub;

  private final LazySingletonInstance<LookerServiceGrpc.LookerServiceBlockingStub>
      lookerServiceStub;

  public LookerFactory(SdmAgentConfig config) {
    super(LOG, config.getLooker(), config.getAuth());
    metricServiceStub =
        new LazySingletonInstance<>(() -> MetricsServiceGrpc.newBlockingStub(channel.getInstance()));
    lookerServiceStub =
        new LazySingletonInstance<>(() -> LookerServiceGrpc.newBlockingStub(channel.getInstance()));
  }

  public MetricsServiceGrpc.MetricsServiceBlockingStub metricsServiceStub() {
    return metricServiceStub.getInstance();
  }

  public LookerServiceGrpc.LookerServiceBlockingStub lookerServiceStub() {
    return lookerServiceStub.getInstance();
  }
}
