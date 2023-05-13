package io.spoud.agoora.agents.kafka.data;

import io.spoud.agoora.agents.kafka.Constants;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.sdm.logistics.domain.v1.DataSubscriptionState;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;

@Slf4j
@ApplicationScoped
public class KafkaConsumerGroupMapper {

  private final String bootstrapServers;

  public KafkaConsumerGroupMapper(KafkaAgentConfig config) {
    bootstrapServers = config.kafka().bootstrapServers();
  }

  public static Optional<String> getConsumerGroupName(Map<String, String> properties) {
    return Optional.ofNullable(properties.get(Constants.AGOORA_PROPERTIES_KAFKA_CONSUMER_GROUP));
  }

  public KafkaConsumerGroup create(final String consumerGroupName, final String topicName) {
    return KafkaConsumerGroup.builder()
        .consumerGroupName(consumerGroupName)
        .topicName(topicName)
        .transportUrl(
            "kafka://"
                + bootstrapServers
                + "?topic="
                + topicName
                + "&consumerGroup="
                + consumerGroupName)
        .properties(
            Map.of(
                Constants.AGOORA_PROPERTIES_KAFKA_TOPIC,
                topicName,
                Constants.AGOORA_PROPERTIES_KAFKA_CONSUMER_GROUP,
                consumerGroupName))
        .build();
  }

  public Optional<KafkaConsumerGroup> create(final DataSubscriptionState dataSubscriptionState) {
    return KafkaTopicMapper.getTopicName(dataSubscriptionState.getPropertiesMap())
        .flatMap(
            topicName ->
                KafkaConsumerGroupMapper.getConsumerGroupName(
                        dataSubscriptionState.getPropertiesMap())
                    .map(
                        consumerGroupName ->
                            KafkaConsumerGroup.builder()
                                .dataSubscriptionStateId(dataSubscriptionState.getId())
                                .dataPortId(dataSubscriptionState.getDataPort().getId())
                                .topicName(topicName)
                                .consumerGroupName(consumerGroupName)
                                .deleted(dataSubscriptionState.getDeleted())
                                .transportUrl(dataSubscriptionState.getTransportUrl())
                                .properties(dataSubscriptionState.getPropertiesMap())
                                .build()));
  }
}
