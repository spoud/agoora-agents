package io.spoud.agoora.agents.pgsql;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.spoud.agoora.agents.api.auth.AuthClientInterceptor;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.hooks.domain.v1.LogRecord;
import io.spoud.sdm.hooks.domain.v1.StateChangeAction;
import io.spoud.sdm.logistics.domain.v1.DataOffer;
import io.spoud.sdm.logistics.domain.v1.DataPort;
import io.spoud.sdm.logistics.domain.v1.DataSubscriptionState;
import io.spoud.sdm.logistics.domain.v1.ResourceGroup;
import io.spoud.sdm.logistics.domain.v1.Transport;

/**
 * Put in there all the classes that are needed at runtime but are not referenced directly (for
 * example only by reflexion). This is needed to compile the native binary
 */
@RegisterForReflection(
    targets = {
      // Entities
      LogRecord.class,
      Timestamp.class,
      Timestamp.Builder.class,
      StringValue.class,
      StringValue.Builder.class,
      StateChangeAction.Type.class,
      ResourceEntity.Type.class,
      DataOffer.class,
      DataPort.class,
      DataSubscriptionState.class,
      ResourceGroup.class,
      Transport.class,


      AuthClientInterceptor.class,

      //      org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl.class,

      //            org.apache.commons.logging.LogFactory.class,
      //            org.apache.commons.logging.impl.LogFactoryImpl.class,
      //            org.apache.commons.logging.impl.SimpleLog.class

      //            AuthClientInterceptor.class,
      //
      //      // rest easy dependencies
      //      org.apache.commons.logging.LogFactory.class,
      //            org.apache.commons.logging.impl.LogFactoryImpl.class,
      //      org.apache.commons.logging.impl.SimpleLog.class,
      //      javax.ws.rs.client.ClientBuilder.class,
      //      org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder.class,
      //      org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl.class,
      //      org.jboss.resteasy.client.jaxrs.internal.proxy.ProxyBuilderImpl.class,
      //      //      org.jboss.resteasy.client.jaxrs.internal.proxy.ClientInvoker.class,
      //      org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration.class,
      //      org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration.class,
      //      org.jboss.resteasy.spi.ResteasyProviderFactory.class,
      //      org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryDelegate.class,
      //      org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl.class,
      ////      org.jboss.resteasy.client.jaxrs.internal.LocalResteasyProviderFactory.class,
      //      org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider.class,
      //      org.jboss.resteasy.plugins.providers.ServerFormUrlEncodedProvider.class,
      //      TokenApi.class,
    })
public class Reflection {}
