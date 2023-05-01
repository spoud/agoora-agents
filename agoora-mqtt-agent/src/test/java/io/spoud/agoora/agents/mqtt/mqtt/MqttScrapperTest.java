package io.spoud.agoora.agents.mqtt.mqtt;

import com.google.protobuf.StringValue;
import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.agoora.agents.api.client.ProfilerClient;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.mqtt.AbstractService;
import io.spoud.agoora.agents.test.mock.DataPortClientMockProvider;
import io.spoud.agoora.agents.test.mock.ProfilerClientMockProvider;
import io.spoud.agoora.agents.test.mock.SchemaClientMockProvider;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.logistics.service.v1.DataPortChange;
import io.spoud.sdm.logistics.service.v1.SaveDataPortRequest;
import io.spoud.sdm.schema.domain.v1alpha.SchemaEncoding;
import io.spoud.sdm.schema.domain.v1alpha.SchemaSource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@Slf4j
@QuarkusTest
class MqttScrapperTest extends AbstractService {

  private static final String JSON_CONTENT = "{\"field\":1}";

  @Inject
  ProfilerClient profilerClient;
  @Inject
  DataPortClient dataPortClient;
  @Inject
  SchemaClient schemaClient;
  @Inject
  MqttScrapper mqttScrapper;
  @Inject
  MqttClient mqttClient;

  @BeforeEach
  void setupMock() {
    ProfilerClientMockProvider.defaultMock(profilerClient);
    DataPortClientMockProvider.defaultMock(dataPortClient);
    SchemaClientMockProvider.defaultMock(schemaClient);
  }

  @Test
  @Timeout(30)
  void simpleTest() {

    ArgumentCaptor<SaveDataPortRequest> arg = ArgumentCaptor.forClass(SaveDataPortRequest.class);

    final IterationContext iterationContext = mqttScrapper.startIteration();

    publishMessages("path1/hello", JSON_CONTENT, 10);
    publishMessages("path1/hello2/ads", JSON_CONTENT, 10);
    publishMessages("path1/hello2/asdfosu", JSON_CONTENT, 10);
    publishMessages("path2/hello3", JSON_CONTENT, 10);
    publishMessages("path3/sub1/bla", JSON_CONTENT, 10);
    publishMessages("path3/sub1/salut", JSON_CONTENT, 10);

    verify(dataPortClient, timeout(10000).times(4)).save(arg.capture());

    assertThat(arg.getAllValues())
        .hasSize(4)
        .extracting(SaveDataPortRequest::getInput)
        .extracting(DataPortChange::getLabel)
        .extracting(StringValue::getValue)
        .containsExactlyInAnyOrder(
            "path1/hello", "path1/hello2", "path3/sub1/bla", "path3/sub1/salut");

    verify(profilerClient, timeout(10000).times(4)).profileData(any(), any());
    verify(schemaClient, timeout(10000).times(4))
        .saveSchema(
            eq(ResourceEntity.Type.DATA_PORT),
            any(),
            eq("/default/"),
            eq(ProfilerClientMockProvider.SCHEMA),
            eq(SchemaSource.Type.INFERRED),
            eq(SchemaEncoding.Type.JSON),
            eq(""),
            eq(SchemaEncoding.Type.UNKNOWN)
        );

    mqttScrapper.stopRemainingOfPreviousIteration(iterationContext);

    // TODO check metrics
  }

  @SneakyThrows
  private void publishMessages(String topic, String message, int times) {
    IntStream.range(0, times).forEach(unused -> publishMessage(topic, message));
  }

  @SneakyThrows
  private void publishMessage(String topic, String message) {
    mqttClient.publish(topic, new MqttMessage(message.getBytes(StandardCharsets.UTF_8)));
  }
}
