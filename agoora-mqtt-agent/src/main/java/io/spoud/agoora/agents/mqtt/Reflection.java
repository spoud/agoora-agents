package io.spoud.agoora.agents.mqtt;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.quarkus.runtime.annotations.RegisterForReflection;
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

            // rest easy dependencies
            org.apache.commons.logging.LogFactory.class,
            org.apache.commons.logging.impl.SimpleLog.class,
            org.eclipse.paho.client.mqttv3.logging.LoggerFactory.class,

            // eclipse paho
            org.eclipse.paho.client.mqttv3.logging.JSR47Logger.class,
            org.eclipse.paho.client.mqttv3.internal.MessageCatalog.class,
            org.eclipse.paho.client.mqttv3.internal.ResourceBundleCatalog.class,
            java.util.ResourceBundle.class,

            org.eclipse.paho.client.mqttv3.spi.NetworkModuleFactory.class,
            org.eclipse.paho.client.mqttv3.internal.SSLNetworkModuleFactory.class,
            org.eclipse.paho.client.mqttv3.internal.TCPNetworkModuleFactory.class,
            org.eclipse.paho.client.mqttv3.internal.websocket.WebSocketSecureNetworkModuleFactory.class,
            org.eclipse.paho.client.mqttv3.internal.websocket.WebSocketNetworkModuleFactory.class,
            org.eclipse.paho.client.mqttv3.internal.NetworkModuleService.class,
            org.eclipse.paho.client.mqttv3.internal.NetworkModule.class,
            org.eclipse.paho.client.mqttv3.internal.TCPNetworkModule.class,
            org.eclipse.paho.client.mqttv3.internal.SSLNetworkModule.class,
            org.eclipse.paho.client.mqttv3.internal.websocket.WebSocketNetworkModule.class,
            org.eclipse.paho.client.mqttv3.internal.websocket.WebSocketSecureNetworkModule.class,
    })
public class Reflection {}
