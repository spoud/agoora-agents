package io.spoud.agoora.agents.mqtt.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopicDescription {
    private String dataPortTopic;
}
