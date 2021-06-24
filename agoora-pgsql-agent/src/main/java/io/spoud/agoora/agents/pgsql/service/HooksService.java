package io.spoud.agoora.agents.pgsql.service;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.spoud.agoora.agents.api.client.HooksClient;
import io.spoud.agoora.agents.pgsql.config.data.PgsqlAgooraConfig;
import io.spoud.agoora.agents.pgsql.repository.DataItemRepository;
import io.spoud.agoora.agents.pgsql.repository.DataPortRepository;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.hooks.domain.v1.LogRecord;
import io.spoud.sdm.logistics.domain.v1.DataItem;
import io.spoud.sdm.logistics.domain.v1.DataPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.context.ManagedExecutor;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class HooksService {

  private final HooksClient hooksClient;
  private final DataPortRepository dataPortRepository;
  private final DataItemRepository dataItemRepository;
  private final ManagedExecutor managedExecutor;
  private final PgsqlAgooraConfig config;

  void onStart(@Observes StartupEvent ev) {
    if (config.getScrapper().getHooks().isEnabled()) {
      // We delay hooks to let the app starts peacefully
      Uni.createFrom()
          .item("")
          .onItem()
          .delayIt()
          .by(Duration.ofSeconds(5))
          .runSubscriptionOn(managedExecutor)
          .subscribe()
          .with(v -> startListeningToHooks());
    }
  }

  void startListeningToHooks() {
    LOG.info("Start listening to hooks");
    hooksClient.startListening(
        this::logRecordChange,
        config.getTransport().getAgooraPathObject().getAbsolutePath(),
        true,
        true,
        false);
  }

  private void logRecordChange(LogRecord logRecord) {
    try {
      if (logRecord.getEntityType() == ResourceEntity.Type.DATA_PORT) {
        final DataPort dataport = logRecord.getDataPort();
        switch (logRecord.getAction()) {
          case UPDATED:
            LOG.debug("DataPort update: {}", logRecord.getEntityUuid());
            dataPortRepository.update(dataport);
            break;
          case DELETED:
            LOG.debug("DataPort delete: {}", logRecord.getEntityUuid());
            dataPortRepository.deleteById(logRecord.getEntityUuid());
            break;
          default:
            LOG.error("Unknown action type {}", logRecord.getAction());
        }
      } else if (logRecord.getEntityType() == ResourceEntity.Type.DATA_ITEM) {
        final DataItem dataItem = logRecord.getDataItem();
        switch (logRecord.getAction()) {
          case UPDATED:
            LOG.debug("DataItem update: {}", logRecord.getEntityUuid());
            dataItemRepository.update(dataItem);
            break;
          case DELETED:
            LOG.debug("DataItem delete: {}", logRecord.getEntityUuid());
            dataItemRepository.deleteById(logRecord.getEntityUuid());
            break;
          default:
            LOG.error("Unknown action type {}", logRecord.getAction());
        }
      } else {
        LOG.debug("Entity of type {} is ignored", logRecord.getEntityType());
      }
    } catch (Exception ex) {
      LOG.error("Error while processing log record for entity {}", logRecord.getEntityUuid(), ex);
    }
  }
}
