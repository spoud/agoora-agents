package io.spoud.agoora.agents.api.factory;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.spoud.agoora.agents.api.auth.AuthClientInterceptor;
import io.spoud.agoora.agents.api.config.SdmAgentClientAuthConfig;
import io.spoud.agoora.agents.api.config.SdmAgentEndpointConfig;
import io.spoud.agoora.agents.api.utils.LazySingletonInstance;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

public abstract class AbstractGrpcClientFactory implements AutoCloseable {

  protected final LazySingletonInstance<ManagedChannel> channel;
  protected final SdmAgentEndpointConfig endpointConfig;
  private final Logger logger;

  protected AbstractGrpcClientFactory(Logger logger, SdmAgentEndpointConfig endpointConfig) {
    this(logger, endpointConfig, null);
  }

  protected AbstractGrpcClientFactory(
      Logger logger, SdmAgentEndpointConfig endpointConfig, SdmAgentClientAuthConfig authConfig) {
    this.logger = logger;
    this.endpointConfig = endpointConfig;
    channel = new LazySingletonInstance<>(() -> createManagedChannel(endpointConfig, authConfig));
  }

  public void close() throws Exception {
    logger.info("Stopping managed channel to {}", endpointConfig.getEndpoint());
    if (channel.isInstantiated()) {
      final ManagedChannel c = channel.getInstance();
      c.shutdownNow();
      boolean success = c.awaitTermination(5, TimeUnit.SECONDS);
      if (!success) {
        logger.error("Unable to close client to {}", endpointConfig.getEndpoint());
      }
    }
  }

  public ManagedChannel createManagedChannel(
      SdmAgentEndpointConfig endpointConfig, SdmAgentClientAuthConfig authConfig) {
    if (endpointConfig == null) {
      throw new IllegalArgumentException(
          "No Endpoint configuration provided for " + logger.getName());
    }
    ManagedChannelBuilder<?> mcb = ManagedChannelBuilder.forTarget(endpointConfig.getEndpoint());

    if (endpointConfig.isInsecure()) {
      mcb.usePlaintext();
    }

    if (authConfig != null) {
      AuthClientInterceptor interceptor = new AuthClientInterceptor(authConfig);
      mcb.intercept(interceptor);

      logger.info(
          "Channel build: endpoint={}, insecure={}, authUsername={}, authServer={}, authRealm={}",
          endpointConfig.getEndpoint(),
          endpointConfig.isInsecure(),
          authConfig.getUser().getName(),
          authConfig.getServerUrl(),
          authConfig.getRealm());
    } else {
      logger.info(
          "Channel build: endpoint={}, insecure={}",
          endpointConfig.getEndpoint(),
          endpointConfig.isInsecure());
    }

    return mcb.build();
  }
}
