package io.spoud.agoora.agents.mqtt.repository;

import io.spoud.agoora.agents.mqtt.Constants;
import io.spoud.agoora.agents.mqtt.data.TopicDescription;
import io.spoud.sdm.logistics.domain.v1.DataPort;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class DataPortRepository {
  private final Map<String, DataPort> portsById = new HashMap<>();

  public void update(DataPort state) {
    portsById.put(state.getId(), state);
  }

  public Optional<DataPort> getDataPortByTopicDescription(TopicDescription description) {
    return portsById.values().stream()
        .filter(
            dataPort -> {
              final String value =
                  dataPort.getPropertiesMap().get(Constants.SDM_MATCHING_TOPIC_NAME);
              return value != null && value.equals(description.getDataPortTopic());
            })
        .findAny();
  }
}
