package io.spoud.agoora.agents.kafka.data;

import io.spoud.agoora.agents.kafka.Constants;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.sdm.logistics.domain.v1.DataPort;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;

@Slf4j
@ApplicationScoped
public class KafkaTopicMapper {

  private final String bootstrapServers;

  public KafkaTopicMapper( KafkaAgentConfig config) {
    bootstrapServers = config.getKafka().getBootstrapServers();
  }

  public static Optional<String> getTopicName(Map<String, String> properties) {
    return Optional.ofNullable(
        properties.get(Constants.SDM_PROPERTIES_KAFKA_TOPIC));
  }

  public KafkaTopic create(final String topicName, int partitionCount) {
    return KafkaTopic.builder()
        .topicName(topicName)
        .transportUrl("kafka://" + bootstrapServers + "?topic=" + topicName)
        .properties(
            Map.of(
                    Constants.SDM_PROPERTIES_KAFKA_TOPIC,
                topicName,
                    Constants.SDM_PROPERTIES_KAFKA_TOPIC_PARTITON_COUNT,
                String.valueOf(partitionCount)))
        .build();
  }

  public Optional<KafkaTopic> create(final DataPort dataPort) {
    return getTopicName(dataPort.getPropertiesMap())
        .map(
            topicName ->
                KafkaTopic.builder()
                    .dataPortId(dataPort.getId())
                    .topicName(topicName)
                    .deleted(dataPort.getDeleted())
                    .transportUrl(dataPort.getEndpointUrl())
                    .properties(dataPort.getPropertiesMap())
                    .build());
  }
}
