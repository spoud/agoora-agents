package io.spoud.agoora.agents.kafka;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class Constants {
  public static final String PROPETIES_DEEP_DIVE_TOOL_SCHEMA_REGISTRY =
      "sdm.transport.external.schema-registry.url";

  public static final String AGOORA_MATCHING_TOPIC_NAME = "sdm.transport.kafka.topic.name";

  public static final String SDM_PROPERTIES_TRANSPORT_AGENT_PREFIX = "sdm.transport.";
  public static final String SDM_PROPERTIES_KAFKA_CONSUMER_GROUP =
          SDM_PROPERTIES_TRANSPORT_AGENT_PREFIX + "kafka.consumerGroup.name";
  public static final String SDM_PROPERTIES_KAFKA_TOPIC =
          SDM_PROPERTIES_TRANSPORT_AGENT_PREFIX + "kafka.topic.name";
  public static final String SDM_PROPERTIES_KAFKA_TOPIC_PARTITON_COUNT =
          SDM_PROPERTIES_TRANSPORT_AGENT_PREFIX + "kafka.topic.partitionCount";

  public static final String SDM_EXTERNAL_PROPERTIES_PREFIX =
          SDM_PROPERTIES_TRANSPORT_AGENT_PREFIX + "external.";

  public static final String SDM_TEMPLATE_KAFKA_CONSUMER_GROUP = "{CONSUMER_GROUP_NAME}";
  public static final String SDM_TEMPLATE_KAFKA_TOPIC = "{TOPIC_NAME}";
  public static final String SDM_TEMPLATE_RESOURCE_ID = "{RESOURCE_ID}";
}
