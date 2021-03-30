package io.spoud.agoora.agents.kafka.repository;

import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import io.spoud.agoora.agents.kafka.data.KafkaTopicMapper;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.hooks.domain.v1.LogRecord;
import io.spoud.sdm.hooks.domain.v1.StateChangeAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class KafkaTopicRepository {

  private final Map<String, KafkaTopic> statesByDataPortId = new ConcurrentHashMap<>();
  private final Map<String, KafkaTopic> statesByInternalId = new ConcurrentHashMap<>();
  private final KafkaTopicMapper topicMapper;

  public Collection<KafkaTopic> getStates() {
    return Collections.unmodifiableCollection(new HashSet<>(statesByDataPortId.values()));
  }

  public void save(KafkaTopic topic) {
    statesByDataPortId.put(topic.getDataPortId(), topic);
    statesByDataPortId.put(topic.getInternalId(), topic);
  }

  public void delete(KafkaTopic topic) {
    statesByDataPortId.remove(topic.getDataPortId());
    statesByInternalId.remove(topic.getInternalId());
  }

  public void onNext(LogRecord logRecord) {
    if (logRecord.getEntityType() == ResourceEntity.Type.DATA_PORT) {
      if (logRecord.getAction() == StateChangeAction.Type.UPDATED && !logRecord.getDataPort().getDeleted()) {
        topicMapper
            .create(logRecord.getDataPort())
            .ifPresentOrElse(
                kafkaTopic -> {
                  LOG.debug(
                      "Got update for DataPort '{}/{}' from Logistics. Mapped to KafkaTopic '{}'.",
                      logRecord.getEntityUuid(),
                      logRecord.getDataPort().getName(),
                      kafkaTopic.getTopicName());
                  statesByDataPortId.put(logRecord.getEntityUuid(), kafkaTopic);
                  statesByInternalId.put(kafkaTopic.getInternalId(), kafkaTopic);
                },
                () ->
                    LOG.warn(
                        "Could not map DataPort '{}/{}' from Logistics because of missing properties to match against a topic. Got properties: {}.",
                        logRecord.getEntityUuid(),
                        logRecord.getDataPort().getName(),
                        logRecord.getDataPort().getPropertiesMap()));

      } else if (logRecord.getAction() == StateChangeAction.Type.DELETED) {
        KafkaTopic dos = statesByDataPortId.get(logRecord.getEntityUuid());
        if (dos != null) {
          statesByInternalId.remove(dos.getInternalId());
        }
        statesByDataPortId.remove(logRecord.getEntityUuid());
      }
    }
  }
}
