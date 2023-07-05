package io.spoud.agoora.agents.pgsql.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.StringValue;
import io.spoud.agoora.agents.api.client.DataItemClient;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.agoora.agents.api.client.MetricsClient;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.pgsql.Constants;
import io.spoud.agoora.agents.pgsql.config.data.PgsqlAgooraConfig;
import io.spoud.agoora.agents.pgsql.data.DatabaseDescription;
import io.spoud.agoora.agents.pgsql.data.JsonSchema;
import io.spoud.agoora.agents.pgsql.data.TableDescription;
import io.spoud.agoora.agents.pgsql.database.DatabaseScrapper;
import io.spoud.agoora.agents.pgsql.repository.DataItemRepository;
import io.spoud.agoora.agents.pgsql.repository.DataPortRepository;
import io.spoud.agoora.agents.pgsql.utils.SchemaUtil;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.global.selection.v1.IdPathRef;
import io.spoud.sdm.logistics.domain.v1.DataItem;
import io.spoud.sdm.logistics.domain.v1.DataPort;
import io.spoud.sdm.logistics.mutation.v1.PropertyMap;
import io.spoud.sdm.logistics.mutation.v1.StateChange;
import io.spoud.sdm.logistics.selection.v1.DataItemRef;
import io.spoud.sdm.logistics.selection.v1.DataPortRef;
import io.spoud.sdm.logistics.selection.v1.ResourceGroupRef;
import io.spoud.sdm.logistics.selection.v1.TransportMatchingProperties;
import io.spoud.sdm.logistics.service.v1.DataItemChange;
import io.spoud.sdm.logistics.service.v1.DataPortChange;
import io.spoud.sdm.logistics.service.v1.SaveDataItemRequest;
import io.spoud.sdm.logistics.service.v1.SaveDataPortRequest;
import io.spoud.sdm.looker.domain.v1alpha1.ResourceMetricType;
import io.spoud.sdm.schema.domain.v1alpha.SchemaEncoding;
import io.spoud.sdm.schema.domain.v1alpha.SchemaSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;

import static io.spoud.agoora.agents.api.mapper.StandardProtoMapper.stringValue;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class DataService {

  private final DataPortRepository dataPortRepository;
  private final DataItemRepository dataItemRepository;
  private final DatabaseScrapper databaseScrapper;
  private final ObjectMapper objectMapper;
  private final ReferenceService referenceService;
  private final DataPortClient dataPortClient;
  private final DataItemClient dataItemClient;
  private final SchemaClient schemaClient;
  private final MetricsClient metricsClient;

  private final PgsqlAgooraConfig config;

  public void updateStates() {
    // TODO diff with hooks and repository
    try {

      final DatabaseDescription databaseDescription = databaseScrapper.getDatabaseDescription();
      LOG.info(
          "{} tables found for database '{}'",
          databaseDescription.getTables().size(),
          databaseDescription.getName());

      final DataPort dataPort = uploadDataPort(databaseDescription);
      dataPortRepository.update(dataPort);

      databaseDescription
          .getTables()
          .forEach(
              table -> {
                DataItem dataItem = uploadDataItem(table, dataPort);
                dataItemRepository.update(dataItem);
                uploadSchema(table, dataItem);
                uploadMetrics(table, dataItem);
              });
    } catch (Exception ex) {
      LOG.error("Error while updating the data port", ex);
    }
  }

  private DataPort uploadDataPort(final DatabaseDescription database) {
    StringValue tableName = stringValue(database.getName());

    Map<String, String> matchingProperties =
        Map.of(Constants.AGOORA_MATCHING_DATABASE_NAME, database.getName());
    Map<String, String> allProperties = new HashMap<>(matchingProperties);
    allProperties.put(Constants.AGOORA_DATABASE_URL, referenceService.getDatabaseUrl());

    return dataPortClient.save(
        SaveDataPortRequest.newBuilder()
            .setInput(
                DataPortChange.newBuilder()
                    .setSelf(
                        DataPortRef.newBuilder()
                            .setTransportMatchingProperties(
                                TransportMatchingProperties.newBuilder()
                                    .setTransport(referenceService.getTransportRef())
                                    .putAllProperties(matchingProperties)
                                    .build())
                            .build())
                    .setState(StateChange.AVAILABLE)
                    .setLabel(tableName)
                    .setTransportUrl(tableName)
                    .setResourceGroup(
                        ResourceGroupRef.newBuilder()
                            .setIdPath(
                                IdPathRef.newBuilder()
                                    .setPath(
                                        config
                                            .transport()
                                            .getAgooraPathObject()
                                            .getResourceGroupPath())
                                    .buildPartial())
                            .build())
                    .setProperties(PropertyMap.newBuilder().putAllProperties(allProperties).build())
                    .setTransport(referenceService.getTransportRef())
                    .build())
            .build());
  }

  private DataItem uploadDataItem(final TableDescription table, final DataPort dataPort) {
    StringValue tableName = stringValue(table.getName());

    Map<String, String> matchingProperties =
        Map.of(Constants.AGOORA_MATCHING_TABLE_NAME, table.getName());
    Map<String, String> allProperties = new HashMap<>(matchingProperties);

    return dataItemClient.save(
        SaveDataItemRequest.newBuilder()
            .setInput(
                DataItemChange.newBuilder()
                    .setSelf(
                        DataItemRef.newBuilder()
                            .setTransportMatchingProperties(
                                TransportMatchingProperties.newBuilder()
                                    .setTransport(referenceService.getTransportRef())
                                    .putAllProperties(matchingProperties)
                                    .build())
                            .build())
                    .setState(StateChange.AVAILABLE)
                    .setLabel(tableName)
                    .setTransportUrl(tableName)
                    .setDataPortRef(
                        DataPortRef.newBuilder()
                            .setIdPath(IdPathRef.newBuilder().setId(dataPort.getId()).build())
                            .build())
                    .setProperties(PropertyMap.newBuilder().putAllProperties(allProperties).build())
                    .build())
            .build());
  }

  private void uploadSchema(TableDescription table, DataItem dataItem) {

    try {
      JsonSchema jsonSchema = SchemaUtil.convertToJsonSchema(table);
      String schemaContent = objectMapper.writeValueAsString(jsonSchema);

      String schemaId =
          schemaClient
              .saveSchema(
                  ResourceEntity.Type.DATA_ITEM,
                  dataItem.getId(),
                  config.transport().getAgooraPathObject().getResourceGroupPath(),
                  schemaContent,
                  SchemaSource.Type.INFERRED,
                  SchemaEncoding.Type.JSON,
                  "",
                  SchemaEncoding.Type.UNKNOWN)
              .getId();
      LOG.info("Schema {} saved for data item {}", schemaId, dataItem.getId());
    } catch (Exception ex) {
      LOG.error("Unable to send schema for data item {}", dataItem.getId(), ex);
    }
  }

  private void uploadMetrics(TableDescription table, DataItem dataItem) {
    databaseScrapper
        .getRowCount(table.getName())
        .ifPresent(
            rowCount -> {
              LOG.debug("Table {} has {} rows", table.getName(), rowCount);
              uploadMetric(
                  dataItem.getId(),
                  ResourceMetricType.Type.DATA_PORT_DATASET_COUNT,
                  rowCount); // TODO be more generic and use data item
            });
    databaseScrapper
        .getTableSizeBytes(table.getName())
        .ifPresent(
            bytes -> {
              LOG.debug("Table {} size is {} bytes", table.getName(), bytes);
              uploadMetric(
                  dataItem.getId(), ResourceMetricType.Type.DATA_PORT_DATASET_SIZE_BYTES, bytes);
            });
    databaseScrapper
        .getChangesCount(table.getName())
        .ifPresent(
            count -> {
              LOG.debug("Table {} changes count is {}", table.getName(), count);
              uploadMetric(dataItem.getId(), ResourceMetricType.Type.DATA_PORT_MUTATIONS, count);
            });
  }

  private void uploadMetric(String resourceId, ResourceMetricType.Type type, double value) {
    try {
      metricsClient.updateMetric(resourceId, type, value);
    } catch (Exception ex) {
      LOG.error("Unable to send metric for resourceId {} and type {}", resourceId, type, ex);
    }
  }
}
