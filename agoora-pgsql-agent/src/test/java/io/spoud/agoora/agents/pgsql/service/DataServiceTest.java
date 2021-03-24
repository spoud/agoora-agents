package io.spoud.agoora.agents.pgsql.service;

import com.google.protobuf.StringValue;
import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.DataItemClient;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.agoora.agents.api.client.MetricsClient;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.test.mock.DataItemClientMockProvider;
import io.spoud.agoora.agents.test.mock.DataPortClientMockProvider;
import io.spoud.agoora.agents.test.mock.MetricsClientMockProvider;
import io.spoud.agoora.agents.test.mock.SchemaClientMockProvider;
import io.spoud.agoora.agents.pgsql.repository.DataPortRepository;
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
import io.spoud.sdm.looker.v1alpha1.ResourceMetric;
import io.spoud.sdm.schema.domain.v1alpha.SchemaEncoding;
import io.spoud.sdm.schema.domain.v1alpha.SchemaSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@QuarkusTest()
class DataServiceTest {

  private static final String SCHEMA_1 =
      "{\"properties\":{\"installed_rank\":{\"type\":\"int4\"},\"version\":{\"type\":\"varchar\"},\"description\":{\"type\":\"varchar\"},\"type\":{\"type\":\"varchar\"},\"script\":{\"type\":\"varchar\"},\"checksum\":{\"type\":\"int4\"},\"installed_by\":{\"type\":\"varchar\"},\"installed_on\":{\"type\":\"timestamp\"},\"execution_time\":{\"type\":\"int4\"},\"success\":{\"type\":\"bool\"}},\"required\":[\"installed_rank\",\"description\",\"type\",\"script\",\"installed_by\",\"installed_on\",\"execution_time\",\"success\"]}";
  private static final String SCHEMA_2 =
      "{\"properties\":{\"address_uuid\":{\"type\":\"uuid\"},\"label\":{\"type\":\"varchar\"},\"line1\":{\"type\":\"varchar\"},\"line2\":{\"type\":\"varchar\"},\"meta\":{\"type\":\"text\"},\"created\":{\"type\":\"timestamp\"},\"updated\":{\"type\":\"timestamp\"},\"created_by\":{\"type\":\"varchar\"},\"updated_by\":{\"type\":\"varchar\"},\"city_uuid\":{\"type\":\"uuid\"}},\"required\":[\"address_uuid\",\"label\",\"line1\",\"created\",\"updated\",\"created_by\",\"updated_by\",\"city_uuid\"]}";
  private static final String SCHEMA_3 =
      "{\"properties\":{\"city_uuid\":{\"type\":\"uuid\"},\"label\":{\"type\":\"varchar\"},\"meta\":{\"type\":\"text\"},\"created\":{\"type\":\"timestamp\"},\"updated\":{\"type\":\"timestamp\"},\"created_by\":{\"type\":\"varchar\"},\"updated_by\":{\"type\":\"varchar\"}},\"required\":[\"city_uuid\",\"label\",\"created\",\"updated\",\"created_by\",\"updated_by\"]}";
  @Inject
  DataService dataService;
  @Inject ReferenceService referenceService;
  @Inject DataPortRepository dataPortRepository;
  @Inject DataPortClient dataPortClient;
  @Inject DataItemClient dataItemClient;
  @Inject SchemaClient schemaClient;
  @Inject MetricsClient metricsCLient;

  @BeforeEach
  void setup() {
    DataPortClientMockProvider.defaultMock(dataPortClient);
    DataItemClientMockProvider.defaultMock(dataItemClient);
    SchemaClientMockProvider.defaultMock(schemaClient);
    MetricsClientMockProvider.defaultMock(metricsCLient);
  }

  @AfterEach
  void tearDown() {
    dataPortRepository.clear();
    reset(dataPortClient); // TODO reset in lib
  }

  @Test
  void testStateService() {
    dataService.updateStates();

    // State Service should starts after a few seconds
    await()
        .atMost(Duration.ofSeconds(5))
        .until(() -> mockingDetails(dataPortClient).getInvocations().size() >= 1);
    await()
        .atMost(Duration.ofSeconds(5))
        .until(() -> mockingDetails(dataItemClient).getInvocations().size() >= 3);

    verify(dataPortClient).save(eq(createDataPortRequestFor("postgres", "/default/", "/default/postgres")));

    final String dataPortUuid = DataPortClientMockProvider.lastUuid.toString();

    verify(dataItemClient)
        .save(eq(createDataItemRequestFor("flyway_schema_history", dataPortUuid)));
    verify(dataItemClient).save(eq(createDataItemRequestFor("t_city", dataPortUuid)));
    verify(dataItemClient).save(eq(createDataItemRequestFor("t_address", dataPortUuid)));

    verify(schemaClient)
        .saveSchema(
            eq(ResourceEntity.Type.DATA_ITEM),
            anyString(),
            eq("/default/"),
            eq(SCHEMA_1),
            eq(SchemaSource.Type.INFERRED),
            eq(SchemaEncoding.Type.JSON));
    verify(schemaClient)
        .saveSchema(
            eq(ResourceEntity.Type.DATA_ITEM),
            anyString(),
            eq("/default/"),
            eq(SCHEMA_2),
            eq(SchemaSource.Type.INFERRED),
            eq(SchemaEncoding.Type.JSON));
    verify(schemaClient)
        .saveSchema(
            eq(ResourceEntity.Type.DATA_ITEM),
            anyString(),
            eq("/default/"),
            eq(SCHEMA_3),
            eq(SchemaSource.Type.INFERRED),
            eq(SchemaEncoding.Type.JSON));

    verify(metricsCLient, times(2))
        .updateMetric(anyString(), eq(ResourceMetric.MetricType.DATA_PORT_DATASET_COUNT), eq(2.0));
    verify(metricsCLient)
        .updateMetric(anyString(), eq(ResourceMetric.MetricType.DATA_PORT_DATASET_COUNT), eq(3.0));
    verify(metricsCLient)
        .updateMetric(
            anyString(), eq(ResourceMetric.MetricType.DATA_PORT_DATASET_SIZE_BYTES), eq(49152.0));
    verify(metricsCLient)
        .updateMetric(
            anyString(), eq(ResourceMetric.MetricType.DATA_PORT_DATASET_SIZE_BYTES), eq(81920.0));
    verify(metricsCLient)
        .updateMetric(
            anyString(), eq(ResourceMetric.MetricType.DATA_PORT_DATASET_SIZE_BYTES), eq(65536.0));
  }

  private SaveDataPortRequest createDataPortRequestFor(
      String table, String path, String transportPath) {
    StringValue tableStr = StringValue.of(table);
    return SaveDataPortRequest.newBuilder()
        .setInput(
            DataPortChange.newBuilder()
                .setSelf(
                    DataPortRef.newBuilder()
                        .setTransportMatchingProperties(
                            TransportMatchingProperties.newBuilder()
                                .setTransport(referenceService.getTransportRef())
                                .putProperties("sdm.transport.pgsql.database.name", table)
                                .build())
                        .build())
                .setLabel(tableStr)
                .setResourceGroup(
                    ResourceGroupRef.newBuilder()
                        .setIdPath(IdPathRef.newBuilder().setPath(path).build())
                        .build())
                .setTransport(
                    BaseRef.newBuilder()
                        .setIdPath(IdPathRef.newBuilder().setPath(transportPath).build())
                        .build())
                .setState(StateChange.AVAILABLE)
                .setTransportUrl(tableStr)
                .setProperties(
                    PropertyMap.newBuilder()
                        .putAllProperties(
                            Map.of(
                                "sdm.transport.pgsql.database.name",
                                table,
                                "sdm.transport.pgsql.database.url",
                                referenceService.getDatabaseUrl()))
                        .build())
                .build())
        .build();
  }

  private SaveDataItemRequest createDataItemRequestFor(String table, String dataPortId) {
    StringValue tableStr = StringValue.of(table);
    return SaveDataItemRequest.newBuilder()
        .setInput(
            DataItemChange.newBuilder()
                .setSelf(
                    DataItemRef.newBuilder()
                        .setTransportMatchingProperties(
                            TransportMatchingProperties.newBuilder()
                                .setTransport(referenceService.getTransportRef())
                                .putProperties("sdm.transport.pgsql.table.name", table)
                                .build())
                        .build())
                .setLabel(tableStr)
                .setDataPortRef(
                    DataPortRef.newBuilder()
                        .setIdPath(IdPathRef.newBuilder().setId(dataPortId).build())
                        .build())
                .setState(StateChange.AVAILABLE)
                .setTransportUrl(tableStr)
                .setProperties(
                    PropertyMap.newBuilder()
                        .putAllProperties(Map.of("sdm.transport.pgsql.table.name", table))
                        .build())
                .build())
        .build();
  }
}
