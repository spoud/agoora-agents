package io.spoud.agoora.agents.kafka.service;

import com.google.protobuf.StringValue;
import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.agoora.agents.api.client.DataSubscriptionStateClient;
import io.spoud.agoora.agents.kafka.AbstractService;
import io.spoud.agoora.agents.kafka.data.KafkaConsumerGroup;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import io.spoud.agoora.agents.kafka.repository.KafkaConsumerGroupRepository;
import io.spoud.agoora.agents.kafka.repository.KafkaTopicRepository;
import io.spoud.agoora.agents.kafka.utils.KafkaUtils;
import io.spoud.agoora.agents.test.mock.DataPortClientMockProvider;
import io.spoud.agoora.agents.test.mock.DataSubscriptionStateClientMockProvider;
import io.spoud.sdm.logistics.mutation.v1.StateChange;
import io.spoud.sdm.logistics.service.v1.DataPortChange;
import io.spoud.sdm.logistics.service.v1.DataSubscriptionStateChange;
import io.spoud.sdm.logistics.service.v1.SaveDataPortRequest;
import io.spoud.sdm.logistics.service.v1.SaveDataSubscriptionStateRequest;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@QuarkusTest
class DataServiceTest extends AbstractService {

  @Inject KafkaUtils kafkaUtils;
  @Inject AdminClient adminClient;
  @Inject DataPortClient dataPortClient;
  @Inject DataSubscriptionStateClient dataSubscriptionStateClient;
  @Inject DataService dataService;
  @Inject KafkaTopicRepository kafkaTopicRepository;
  @Inject KafkaConsumerGroupRepository kafkaConsumerGroupRepository;

  @BeforeEach
  void setup() {
    DataPortClientMockProvider.defaultMock(dataPortClient);
    DataSubscriptionStateClientMockProvider.defaultMock(dataSubscriptionStateClient);
  }

  @AfterEach
  void tearDown() {
    kafkaUtils.cleanup();
    kafkaTopicRepository.clear();
    kafkaConsumerGroupRepository.clear();
    reset(dataSubscriptionStateClient);
  }

  @Test
  @Timeout(30)
  void testDataPorts() {
    kafkaTopicRepository.save(
        KafkaTopic.builder().dataPortId("to-remove-abc").topicName("data-topicX").build());

    adminClient.createTopics(
        Arrays.asList(
            new NewTopic("data-topic1", 2, (short) 1),
            new NewTopic("data-topic2", 3, (short) 1),
            new NewTopic("data-topic3", 4, (short) 1)));

    await()
        .atMost(Duration.ofSeconds(5))
        .until(
            () ->
                adminClient
                    .listTopics()
                    .names()
                    .get()
                    .containsAll(Arrays.asList("data-topic1", "data-topic2", "data-topic3")));

    dataService.updateTopics();

    ArgumentCaptor<SaveDataPortRequest> captor = ArgumentCaptor.forClass(SaveDataPortRequest.class);
    verify(dataPortClient, timeout(5000).times(4)).save(captor.capture());

    final List<SaveDataPortRequest> requests = captor.getAllValues();
    assertThat(requests)
        .extracting(SaveDataPortRequest::getInput)
        .extracting(DataPortChange::getLabel)
        .extracting(StringValue::getValue)
        .containsExactlyInAnyOrder("data-topic1", "data-topic2", "data-topic3", "");

    final SaveDataPortRequest topicXRequest =
        requests.stream()
            .filter(r -> r.getInput().getSelf().getIdPath().getId().equals("to-remove-abc"))
            .findAny()
            .get();
    assertThat(topicXRequest.getInput().getState()).isEqualTo(StateChange.DELETED);

    assertThat(captor.getAllValues().get(0).getInput().getProperties().getPropertiesMap())
        .containsAllEntriesOf(
            Map.of(
                "sdm.transport.external.kafka.manager.url",
                "https://km.sdm.spoud.io/clusters/sdm/topics/data-topic1",
                "sdm.transport.external.agoora.url",
                "https://blabla/"));
  }

  @Test
  @Timeout(30)
  void testDataSubscriptionState() {
    kafkaTopicRepository.save(
        KafkaTopic.builder().dataPortId("abc").topicName("data-topic1").build());
    kafkaConsumerGroupRepository.save(
        KafkaConsumerGroup.builder()
            .dataSubscriptionStateId("to-delete-subId")
            .consumerGroupName("groupX")
            .dataPortId("abc")
            .topicName("data-topic1")
            .build());

    adminClient.createTopics(
        Arrays.asList(
            new NewTopic("data-topic1", 2, (short) 1),
            new NewTopic("data-topic2", 3, (short) 1),
            new NewTopic("data-topic3", 4, (short) 1)));

    final byte[] data = "data".getBytes(StandardCharsets.UTF_8);

    // produce some data
    Arrays.asList("data-topic1", "data-topic2", "data-topic3")
        .forEach(
            topic -> {
              for (int i = 0; i < 10; i++) {
                kafkaUtils.produce(topic, data);
              }
            });

    // 2 groups but will result in 3 data subscription state
    assertThat(kafkaUtils.consume("data-topic1", "data-group1", 10, Duration.ofSeconds(10)))
        .hasSize(10);
    assertThat(kafkaUtils.consume("data-topic2", "data-group1", 10, Duration.ofSeconds(10)))
        .hasSize(10);
    assertThat(kafkaUtils.consume("data-topic1", "data-group2", 10, Duration.ofSeconds(10)))
        .hasSize(10);

    dataService.updateConsumerGroups();

    ArgumentCaptor<SaveDataSubscriptionStateRequest> captor =
        ArgumentCaptor.forClass(SaveDataSubscriptionStateRequest.class);
    verify(dataSubscriptionStateClient, timeout(5000).atLeast(4)).save(captor.capture());

    final List<SaveDataSubscriptionStateRequest> requests =
        captor.getAllValues().stream()
            .filter(req -> req.getInput().getLabel().getValue().startsWith("data-group"))
            .collect(Collectors.toList());
    assertThat(requests)
        .extracting(SaveDataSubscriptionStateRequest::getInput)
        .extracting(DataSubscriptionStateChange::getLabel)
        .extracting(StringValue::getValue)
        .containsExactlyInAnyOrder("data-group1", "data-group1", "data-group2");

    // check deleted entities
    final Optional<SaveDataSubscriptionStateRequest> deleted =
        captor.getAllValues().stream()
            .filter(v -> v.getInput().getState() == StateChange.DELETED)
            .findFirst();
    assertThat(deleted).isPresent();
    assertThat(deleted.get().getInput().getSelf().getIdPath().getId()).isEqualTo("to-delete-subId");

    assertThat(
            captor.getAllValues().stream()
                .filter(v -> v.getInput().getLabel().getValue().startsWith("data-group"))
                .findFirst()
                .get()
                .getInput()
                .getProperties()
                .getPropertiesMap())
        .containsAllEntriesOf(
            Map.of(
                "sdm.transport.external.kafka.manager.url",
                "https://km.sdm.spoud.io/clusters/sdm/consumer-group/data-group1/data-topic1",
                "sdm.transport.external.agoora.url",
                "https://blabla/"));
  }
}
