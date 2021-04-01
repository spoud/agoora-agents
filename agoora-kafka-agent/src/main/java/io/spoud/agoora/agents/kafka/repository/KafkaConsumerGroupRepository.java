package io.spoud.agoora.agents.kafka.repository;

import io.spoud.agoora.agents.kafka.data.KafkaConsumerGroup;
import io.spoud.agoora.agents.kafka.data.KafkaConsumerGroupMapper;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import io.spoud.sdm.hooks.domain.v1.LogRecord;
import io.spoud.sdm.hooks.domain.v1.StateChangeAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class KafkaConsumerGroupRepository {

  private final Map<String, KafkaConsumerGroup> statesByDataSubscriptionStateId =
      new ConcurrentHashMap<>();
  private final Map<String, KafkaConsumerGroup> statesByInternalId = new ConcurrentHashMap<>();
  private final KafkaTopicRepository kafkaTopicRepository;
  private final KafkaConsumerGroupMapper consumerGroupMapper;

  public Collection<KafkaConsumerGroup> getStates() {
    final Set<String> dataPortIds =
        kafkaTopicRepository.getStates().stream()
            .map(KafkaTopic::getDataPortId)
            .collect(Collectors.toSet());

    return Collections.unmodifiableCollection(
        statesByInternalId.values().stream()
            .filter(kafkaConsumerGroup -> dataPortIds.contains(kafkaConsumerGroup.getDataPortId()))
            .collect(Collectors.toSet()));
  }

  public void save(KafkaConsumerGroup consumerGroup) {
    if (consumerGroup.getDataSubscriptionStateId() != null) {
      statesByDataSubscriptionStateId.put(
          consumerGroup.getDataSubscriptionStateId(), consumerGroup);
    }
    statesByInternalId.put(consumerGroup.getInternalId(), consumerGroup);
  }

  public void delete(KafkaConsumerGroup consumerGroup) {
    if (consumerGroup.getDataSubscriptionStateId() != null) {
      statesByDataSubscriptionStateId.remove(consumerGroup.getDataSubscriptionStateId());
    }
    final KafkaConsumerGroup removed = statesByInternalId.remove(consumerGroup.getInternalId());
    if (removed != null && removed.getDataSubscriptionStateId() != null) {
      statesByDataSubscriptionStateId.remove(removed.getDataSubscriptionStateId());
    }
  }

  public void onNext(LogRecord logRecord) {
    if (logRecord.getAction() == StateChangeAction.Type.UPDATED
        && !logRecord.getDataSubscriptionState().getDeleted()) {

      consumerGroupMapper
          .create(logRecord.getDataSubscriptionState())
          .ifPresentOrElse(
              kafkaConsumerGroup -> {
                LOG.info(
                    "Got update for DataSubscriptionState '{}/{}' from Logistics. Mapped to KafkaConsumerGroup '{}/{}'.",
                    logRecord.getEntityUuid(),
                    logRecord.getDataSubscriptionState().getName(),
                    kafkaConsumerGroup.getTopicName(),
                    kafkaConsumerGroup.getConsumerGroupName());
                save(kafkaConsumerGroup);
              },
              () ->
                  LOG.warn(
                      "Could not map DataSubscriptionState '{}/{}' from Logistics because of missing properties to match against a topic/consumerGroup. Got properties: {}.",
                      logRecord.getEntityUuid(),
                      logRecord.getDataSubscriptionState().getName(),
                      logRecord.getDataSubscriptionState().getPropertiesMap()));

    } else if (logRecord.getAction() == StateChangeAction.Type.DELETED) {
      KafkaConsumerGroup kafkaConsumerGroup =
          statesByDataSubscriptionStateId.get(logRecord.getEntityUuid());
      delete(kafkaConsumerGroup);
    }
  }

  public void clear(){
    statesByInternalId.clear();
    statesByDataSubscriptionStateId.clear();
  }
}
