package io.spoud.agoora.agents.kafka;

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

      // Kafka
      org.apache.avro.Schema.class,
      org.apache.avro.Schema.Parser.class,
    })
public class Reflection {}
