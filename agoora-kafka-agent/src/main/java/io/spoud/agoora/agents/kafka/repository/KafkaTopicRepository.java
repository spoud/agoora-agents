package io.spoud.agoora.agents.kafka.repository;

import io.spoud.agoora.agents.api.map.MonitoredConcurrentHashMap;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import io.spoud.agoora.agents.kafka.data.KafkaTopicMapper;
import io.spoud.sdm.hooks.domain.v1.LogRecord;
import io.spoud.sdm.hooks.domain.v1.StateChangeAction;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class KafkaTopicRepository {

  private final Map<String, KafkaTopic> statesByDataPortId = new MonitoredConcurrentHashMap<>("states_by_data_port_id", KafkaTopicRepository.class);
  private final Map<String, KafkaTopic> statesByInternalId = new MonitoredConcurrentHashMap<>("states_by_internal_id", KafkaTopicRepository.class);
  private final KafkaTopicMapper topicMapper;

  public Collection<KafkaTopic> getStates() {
    return Collections.unmodifiableCollection(new HashSet<>(statesByInternalId.values()));
  }

  public void save(KafkaTopic topic) {
    if (topic.getDataPortId() != null) {
      statesByDataPortId.put(topic.getDataPortId(), topic);
    }
    statesByInternalId.put(topic.getInternalId(), topic);
  }

  public void delete(KafkaTopic topic) {
    if (topic.getDataPortId() != null) {
      statesByDataPortId.remove(topic.getDataPortId());
    }
    final KafkaTopic removed = statesByInternalId.remove(topic.getInternalId());
    if (removed != null && removed.getDataPortId() != null) {
      statesByDataPortId.remove(removed.getDataPortId());
    }
  }

  public void onNext(LogRecord logRecord) {
    if (logRecord.getAction() == StateChangeAction.Type.UPDATED
        && !logRecord.getDataPort().getDeleted()) {
      topicMapper
          .create(logRecord.getDataPort())
          .ifPresentOrElse(
              kafkaTopic -> {
                LOG.debug(
                    "Got update for DataPort '{}/{}' from Logistics. Mapped to KafkaTopic '{}'.",
                    logRecord.getEntityUuid(),
                    logRecord.getDataPort().getName(),
                    kafkaTopic.getTopicName());
                save(kafkaTopic);
              },
              () ->
                  LOG.warn(
                      "Could not map DataPort '{}/{}' from Logistics because of missing properties to match against a topic. Got properties: {}.",
                      logRecord.getEntityUuid(),
                      logRecord.getDataPort().getName(),
                      logRecord.getDataPort().getPropertiesMap()));

    } else if (logRecord.getAction() == StateChangeAction.Type.DELETED) {
      KafkaTopic kafkaTopic = statesByDataPortId.get(logRecord.getEntityUuid());
      delete(kafkaTopic);
    }
  }

  public void clear(){
    statesByInternalId.clear();
    statesByDataPortId.clear();
  }
}
