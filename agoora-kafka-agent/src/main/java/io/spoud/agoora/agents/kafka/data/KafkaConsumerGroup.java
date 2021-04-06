package io.spoud.agoora.agents.kafka.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.SortedMap;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KafkaConsumerGroup {
  private String dataSubscriptionStateId;
  private String dataPortId;
  private String consumerGroupName;
  private String topicName;
  private String transportUrl;
  private boolean deleted;
  @Singular private SortedMap<String, String> properties;

  public static String mapInternalId(String topicName, String consumerGroupName) {
    return topicName + "/" + consumerGroupName;
  }

  public String getInternalId() {
    return mapInternalId(this.topicName, this.consumerGroupName);
  }
}
