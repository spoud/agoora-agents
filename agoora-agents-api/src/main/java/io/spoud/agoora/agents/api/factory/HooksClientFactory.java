package io.spoud.agoora.agents.api.factory;

import io.spoud.agoora.agents.api.config.SdmAgentConfig;
import io.spoud.agoora.agents.api.utils.LazySingletonInstance;
import io.spoud.sdm.hooks.v1.StateChangerGrpc;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HooksClientFactory extends AbstractGrpcClientFactory {

  private final LazySingletonInstance<StateChangerGrpc.StateChangerStub> stateChangStub;

  public HooksClientFactory(SdmAgentConfig config) {
    super(LOG, config.getHooks(), config.getAuth());
    stateChangStub = new LazySingletonInstance<>(() -> StateChangerGrpc.newStub(channel.getInstance()));
  }

  public StateChangerGrpc.StateChangerStub stateServiceStub() {
    return stateChangStub.getInstance();
  }
}
