package io.spoud.agoora.agents.openapi.service;

import io.spoud.agoora.agents.api.client.DataItemClient;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.api.mapper.StandardProtoMapper;
import io.spoud.agoora.agents.openapi.Constants;
import io.spoud.agoora.agents.openapi.config.data.OpenApiAgooraConfig;
import io.spoud.agoora.agents.openapi.config.data.OpenApiConfig;
import io.spoud.agoora.agents.openapi.jsonschema.SchemaUtil;
import io.spoud.agoora.agents.openapi.repository.DataItemRepository;
import io.spoud.agoora.agents.openapi.repository.DataPortRepository;
import io.spoud.agoora.agents.openapi.swagger.SwaggerScrapper;
import io.spoud.agoora.agents.openapi.swagger.data.SwaggerOperation;
import io.spoud.agoora.agents.openapi.swagger.data.SwaggerTag;
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
import io.spoud.sdm.schema.domain.v1alpha.SchemaEncoding;
import io.spoud.sdm.schema.domain.v1alpha.SchemaSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class DataService {

  private final DataPortRepository dataPortRepository;
  private final DataItemRepository dataItemRepository;
  private final ReferenceService referenceService;
  private final DataPortClient dataPortClient;
  private final DataItemClient dataItemClient;
  private final SchemaClient schemaClient;

  private final OpenApiAgooraConfig config;

  public void updateStates() {
    final OpenApiConfig openApiCOnfig = config.openapi();
    final SwaggerScrapper swaggerScrapper = new SwaggerScrapper(openApiCOnfig);
    final List<SwaggerTag> tags = swaggerScrapper.getEndpoints();

    LOG.info("{} tags found for url'{}'", tags.size(), openApiCOnfig.url());
    tags.forEach(
        tag -> {
          try {
            final DataPort dataPort = uploadDataPort(tag);
            dataPortRepository.update(dataPort);
            final List<SwaggerOperation> operations = swaggerScrapper.getOperationsForEndpoint(tag);
            LOG.info("{} operations found for endpoint '{}'", operations.size(), tag.getName());
            operations.forEach(
                operation -> {
                  try {
                    final DataItem dataItem = uploadDataItem(operation, dataPort);
                    dataItemRepository.update(dataItem);
                    uploadSchema(operation, dataItem);
                  } catch (Exception ex) {
                    LOG.error(
                        "Unable to update data item for tag {} and operation {} {}",
                        tag,
                        operation.getMethod(),
                        operation.getPath(),
                        ex);
                  }
                });
          } catch (Exception ex) {
            LOG.error("Unable to update data port for tag {}", tag, ex);
          }
        });
  }

  private DataPort uploadDataPort(final SwaggerTag tag) {
    Map<String, String> matchingProperties = Map.of(Constants.AGOORA_OPENAPI_TAG, tag.getName());
    Map<String, String> allProperties = new HashMap<>(matchingProperties);
    allProperties.put(Constants.AGOORA_OPENAPI_URL, tag.getUrl());
    allProperties.put(Constants.AGOORA_DEEP_DIVE_OPENAPI, config.openapi().uiUrl());

    LOG.info("Updating data port with tag name {}", tag.getName());

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
                    .setLabel(StandardProtoMapper.stringValue(tag.getName()))
                    .setTransportUrl(StandardProtoMapper.stringValue(tag.getUrl()))
                    .setResourceGroup(
                        ResourceGroupRef.newBuilder()
                            .setIdPath(
                                IdPathRef.newBuilder()
                                    .setPath(
                                        config.transport().getAgooraPathObject()
                                            .getResourceGroupPath())
                                    .buildPartial())
                            .build())
                    .setProperties(PropertyMap.newBuilder().putAllProperties(allProperties).build())
                    .setTransport(referenceService.getTransportRef())
                    .build())
            .build());
  }

  private DataItem uploadDataItem(final SwaggerOperation operation, final DataPort dataPort) {
    Map<String, String> matchingProperties =
        Map.of(
            Constants.AGOORA_TRANSPORT_OPENAPI_METHOD, operation.getMethod(),
            Constants.AGOORA_OPENAPI_PATH, operation.getPath());
    Map<String, String> allProperties = new HashMap<>(matchingProperties);

    LOG.info("Updating data item with path {} {}", operation.getMethod(), operation.getPath());

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
                    .setLabel(StandardProtoMapper.stringValue(
                        operation.getMethod() + " " + operation.getPath()))
                    .setTransportUrl(StandardProtoMapper.stringValue(operation.getUrl()))
                    .setDataPortRef(
                        DataPortRef.newBuilder()
                            .setIdPath(IdPathRef.newBuilder().setId(dataPort.getId()).build())
                            .build())
                    .setProperties(PropertyMap.newBuilder().putAllProperties(allProperties).build())
                    .build())
            .build());
  }

  private void uploadSchema(final SwaggerOperation operation, DataItem dataItem) {
    try {
      final String schemaContent = SchemaUtil.convertToJsonSchemaString(operation.getSchema());

      String schemaId =
          schemaClient
              .saveSchema(
                  ResourceEntity.Type.DATA_ITEM,
                  dataItem.getId(),
                  config.transport().getAgooraPathObject().getResourceGroupPath(),
                  schemaContent,
                  SchemaSource.Type.REGISTRY,
                  SchemaEncoding.Type.JSON,
                  "",
                  SchemaEncoding.Type.UNKNOWN)
              .getId();
      LOG.info("Schema {} saved for data item {}", schemaId, dataItem.getId());
    } catch (Exception ex) {
      LOG.error("Unable to send schema for data item {}", dataItem.getId(), ex);
    }
  }
}
