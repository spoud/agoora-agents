package io.spoud.agoora.agents.api.factory;

import io.spoud.agoora.agents.api.config.AgooraAgentConfig;
import io.spoud.agoora.agents.api.utils.LazySingletonInstance;
import io.spoud.sdm.profiler.service.v1alpha1.ProfilerGrpc;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProfilerFactory extends AbstractGrpcClientFactory {

  private final LazySingletonInstance<ProfilerGrpc.ProfilerStub> profilerServiceStub;

  public ProfilerFactory(AgooraAgentConfig config) {
    super(LOG, config.getProfiler());
    profilerServiceStub = new LazySingletonInstance<>(() -> ProfilerGrpc.newStub(channel.getInstance()));
  }

  public ProfilerGrpc.ProfilerStub profilerServiceStub() {
    return profilerServiceStub.getInstance();
  }
}
