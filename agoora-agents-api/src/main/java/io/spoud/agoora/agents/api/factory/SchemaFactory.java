package io.spoud.agoora.agents.api.factory;

import io.spoud.agoora.agents.api.config.AgooraAgentConfig;
import io.spoud.agoora.agents.api.utils.LazySingletonInstance;
import io.spoud.sdm.schema.v1alpha.SchemaServiceGrpc;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SchemaFactory extends AbstractGrpcClientFactory {

  private final LazySingletonInstance<SchemaServiceGrpc.SchemaServiceBlockingStub>
      schemaServiceStub;

  public SchemaFactory(AgooraAgentConfig config) {
    super(LOG, config.getSchema(), config.getAuth());
    schemaServiceStub =
        new LazySingletonInstance<>(() -> SchemaServiceGrpc.newBlockingStub(channel.getInstance()));
  }

  public SchemaServiceGrpc.SchemaServiceBlockingStub schemaServiceStub() {
    return schemaServiceStub.getInstance();
  }
}
