package io.spoud.agoora.agents.kafka.service;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.spoud.agoora.agents.api.client.HooksClient;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.agoora.agents.kafka.repository.KafkaConsumerGroupRepository;
import io.spoud.agoora.agents.kafka.repository.KafkaTopicRepository;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.hooks.domain.v1.LogRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.context.ManagedExecutor;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class HooksService {

  private final HooksClient hooksClient;
  private final KafkaTopicRepository kafkaTopicRepository;
  private final KafkaConsumerGroupRepository kafkaConsumerGroupRepository;
  private final ManagedExecutor managedExecutor;
  private final KafkaAgentConfig config;

  void onStart(@Observes StartupEvent ev) {
    if (config.scrapper().hooks().enabled()) {
      // We delay hooks to let the app starts peacefully
      Uni.createFrom()
          .item("")
          .runSubscriptionOn(managedExecutor)
          .subscribe()
          .with(v -> startListeningToHooks());
    }
  }

  void startListeningToHooks() {
    LOG.info("Start listening to hooks");
    hooksClient.startListening(
        this::logRecordChange,
        config.transport().getAgooraPathObject().getAbsolutePath(),
        true,
        false,
        true);
  }

  private void logRecordChange(LogRecord logRecord) {
    try {
      if (logRecord.getEntityType() == ResourceEntity.Type.DATA_PORT) {
        kafkaTopicRepository.onNext(logRecord);
      } else if (logRecord.getEntityType() == ResourceEntity.Type.DATA_SUBSCRIPTION_STATE) {
        kafkaConsumerGroupRepository.onNext(logRecord);
      } else {
        LOG.debug("Entity of type {} is ignored", logRecord.getEntityType());
      }
    } catch (Exception ex) {
      LOG.error("Error while processing log record for entity {}", logRecord.getEntityUuid(), ex);
    }
  }
}
