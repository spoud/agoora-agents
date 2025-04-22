package io.spoud.agoora.agents.openapi.service;

import com.google.protobuf.StringValue;
import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.DataItemClient;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.openapi.repository.DataItemRepository;
import io.spoud.agoora.agents.openapi.repository.DataPortRepository;
import io.spoud.agoora.agents.test.mock.DataItemClientMockProvider;
import io.spoud.agoora.agents.test.mock.DataPortClientMockProvider;
import io.spoud.agoora.agents.test.mock.SchemaClientMockProvider;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.global.selection.v1.BaseRef;
import io.spoud.sdm.global.selection.v1.IdPathRef;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;

import static io.spoud.sdm.global.domain.v1.ResourceEntity.Type.DATA_ITEM;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@QuarkusTest()
class DataServiceTest {

  // using external service to test
  // changes there may break tests
  // https://github.com/swagger-api/swagger-petstore
  private static final String SCHEMA_1 =
      "{\"properties\":{\"produces\":{\"properties\":{\"application/json\":{},\"application/xml\":{},\"application/x-www-form-urlencoded\":{}}},\"requestBody\":{\"type\":\"object\",\"properties\":{\"photoUrls\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"name\":{\"type\":\"string\"},\"id\":{\"type\":\"integer\"},\"category\":{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"id\":{\"type\":\"integer\"}}},\"tags\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"id\":{\"type\":\"integer\"}}}},\"status\":{\"type\":\"string\"}}},\"responses\":{\"properties\":{\"200\":{\"type\":\"object\",\"properties\":{\"photoUrls\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"name\":{\"type\":\"string\"},\"id\":{\"type\":\"integer\"},\"category\":{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"id\":{\"type\":\"integer\"}}},\"tags\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"id\":{\"type\":\"integer\"}}}},\"status\":{\"type\":\"string\"}}},\"400\":{\"type\":\"object\"},\"422\":{\"type\":\"object\"},\"default\":{\"type\":\"object\"}}}}}";
  private static final String SCHEMA_2 =
      "{\"properties\":{\"parameters\":{\"properties\":{\"api_key\":{\"type\":\"string\"},\"petId\":{\"type\":\"integer\"}}},\"responses\":{\"properties\":{\"200\":{\"type\":\"object\"},\"400\":{\"type\":\"object\"},\"default\":{\"type\":\"object\"}}}}}";

  @Inject
  DataService dataService;
  @Inject
  ReferenceService referenceService;
  @Inject
  DataPortRepository dataPortRepository;
  @Inject
  DataItemRepository dataItemRepository;
  @Inject
  DataPortClient dataPortClient;
  @Inject
  DataItemClient dataItemClient;
  @Inject
  SchemaClient schemaClient;

  @BeforeEach
  void setup() {
    DataPortClientMockProvider.defaultMock(dataPortClient);
    DataItemClientMockProvider.defaultMock(dataItemClient);
    SchemaClientMockProvider.defaultMock(schemaClient);
  }

  @AfterEach
  void tearDown() {
    dataPortRepository.clear();
    dataItemRepository.clear();
    reset(dataPortClient); // TODO reset in lib
  }

  @Test
  void testStateService() {
    dataService.updateStates();

    // State Service should start after a few seconds
    await()
        .atMost(Duration.ofSeconds(5))
        .until(() -> !mockingDetails(dataPortClient).getInvocations().isEmpty());
    await()
        .atMost(Duration.ofSeconds(5))
        .until(() -> mockingDetails(dataItemClient).getInvocations().size() >= 3);

    verify(dataPortClient)
        .save(createDataPortRequestFor("pet", "/default/", "/default/openapi"));

    final String dataPortUuid = DataPortClientMockProvider.uuidByLabel.get("pet").toString();

    verify(dataItemClient).save(createDataItemRequestFor("PUT", "/pet", dataPortUuid));
    verify(dataItemClient).save(createDataItemRequestFor("GET", "/pet/{petId}", dataPortUuid));

    final String putDataItem = DataItemClientMockProvider.uuidByTransportUrl.get("/pet").toString();
    final String getDataItem =
        DataItemClientMockProvider.uuidByTransportUrl.get("/pet/{petId}").toString();

    verify(schemaClient)
        .saveSchema(
            ResourceEntity.Type.DATA_ITEM,
            putDataItem,
            "/default/",
            // we are using an external service for testing. changes there may break tests
            SCHEMA_1,
            SchemaSource.Type.REGISTRY,
            SchemaEncoding.Type.JSON,
            "",
            SchemaEncoding.Type.UNKNOWN
        );
    verify(schemaClient)
        .saveSchema(
            ResourceEntity.Type.DATA_ITEM,
            getDataItem,
            "/default/",
            // we are using an external service for testing. changes there may break tests
            SCHEMA_2,
            SchemaSource.Type.REGISTRY,
            SchemaEncoding.Type.JSON,
            "",
            SchemaEncoding.Type.UNKNOWN);
  }

  private SaveDataPortRequest createDataPortRequestFor(
      String tag, String path, String transportPath) {
    return SaveDataPortRequest.newBuilder()
        .setInput(
            DataPortChange.newBuilder()
                .setSelf(
                    DataPortRef.newBuilder()
                        .setTransportMatchingProperties(
                            TransportMatchingProperties.newBuilder()
                                .setTransport(referenceService.getTransportRef())
                                .putProperties("sdm.transport.openapi.tag", tag)
                                .build())
                        .build())
                .setLabel(StringValue.of(tag))
                .setResourceGroup(
                    ResourceGroupRef.newBuilder()
                        .setIdPath(IdPathRef.newBuilder().setPath(path).build())
                        .build())
                .setTransport(
                    BaseRef.newBuilder()
                        .setIdPath(IdPathRef.newBuilder().setPath(transportPath).build())
                        .build())
                .setState(StateChange.AVAILABLE)
                .setTransportUrl(StringValue.of("https://petstore3.swagger.io/api/v3/openapi.json"))
                .setProperties(
                    PropertyMap.newBuilder()
                        .putAllProperties(
                            Map.of(
                                "sdm.transport.openapi.tag",
                                tag,
                                "sdm.transport.external.openapi.url",
                                "https://petstore3.swagger.io",
                                "sdm.transport.openapi.url",
                                "https://petstore3.swagger.io/api/v3/openapi.json"))
                        .build())
                .build())
        .build();
  }

  private SaveDataItemRequest createDataItemRequestFor(
      String method, String path, String dataPortId) {
    return SaveDataItemRequest.newBuilder()
        .setInput(
            DataItemChange.newBuilder()
                .setSelf(
                    DataItemRef.newBuilder()
                        .setTransportMatchingProperties(
                            TransportMatchingProperties.newBuilder()
                                .setTransport(referenceService.getTransportRef())
                                .putAllProperties(
                                    Map.of(
                                        "sdm.transport.openapi.method",
                                        method,
                                        "sdm.transport.openapi.path",
                                        path))
                                .build())
                        .build())
                .setLabel(StringValue.of(method + " " + path))
                .setDataPortRef(
                    DataPortRef.newBuilder()
                        .setIdPath(IdPathRef.newBuilder().setId(dataPortId).build())
                        .build())
                .setState(StateChange.AVAILABLE)
                .setTransportUrl(StringValue.of(path))
                .setProperties(
                    PropertyMap.newBuilder()
                        .putAllProperties(
                            Map.of(
                                "sdm.transport.openapi.method",
                                method,
                                "sdm.transport.openapi.path",
                                path))
                        .build())
                .build())
        .build();
  }
}
