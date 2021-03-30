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
public class KafkaTopic {
  private String dataPortId;
  private String topicName;
  private String transportUrl;
  private boolean deleted;
  @Singular private SortedMap<String, String> properties;

  public static String mapInternalId(String topicName) {
    return topicName;
  }

  public String getInternalId() {
    return mapInternalId(this.topicName);
  }
}
