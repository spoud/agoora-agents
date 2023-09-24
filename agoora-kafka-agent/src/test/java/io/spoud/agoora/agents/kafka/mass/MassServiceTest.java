package io.spoud.agoora.agents.kafka.mass;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.DataPortClient;
import io.spoud.agoora.agents.api.client.DataSubscriptionStateClient;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.kafka.AbstractService;
import io.spoud.agoora.agents.kafka.data.KafkaConsumerGroup;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import io.spoud.agoora.agents.kafka.repository.KafkaConsumerGroupRepository;
import io.spoud.agoora.agents.kafka.repository.KafkaTopicRepository;
import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import io.spoud.agoora.agents.kafka.service.DataService;
import io.spoud.agoora.agents.kafka.utils.KafkaUtils;
import io.spoud.agoora.agents.kafka.utils.SchemaRegistryUtil;
import io.spoud.agoora.agents.test.mock.DataPortClientMockProvider;
import io.spoud.agoora.agents.test.mock.DataSubscriptionStateClientMockProvider;
import io.spoud.agoora.agents.test.mock.SchemaClientMockProvider;
import io.spoud.sdm.logistics.service.v1.SaveDataPortRequest;
import jakarta.inject.Inject;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

// TODO: finish load test and add memory usage test

@QuarkusTest
public class MassServiceTest extends AbstractService {

    @Inject
    KafkaUtils kafkaUtils;
    @Inject
    AdminClient adminClient;
    @Inject
    DataPortClient dataPortClient;
    @Inject
    DataSubscriptionStateClient dataSubscriptionStateClient;
    @Inject
    DataService dataService;
    @Inject
    KafkaTopicRepository kafkaTopicRepository;
    @Inject
    KafkaConsumerGroupRepository kafkaConsumerGroupRepository;
    @Inject
    SchemaClient schemaClient;
    @Inject
    SchemaRegistryUtil schemaRegistryUtil;

    int mass = 500;

    @BeforeEach
    void setup() {
        DataPortClientMockProvider.defaultMock(dataPortClient);
        DataSubscriptionStateClientMockProvider.defaultMock(dataSubscriptionStateClient);
        SchemaClientMockProvider.defaultMock(schemaClient);
    }

    @AfterEach
    void tearDown() {
        kafkaUtils.cleanup();
        kafkaTopicRepository.clear();
        kafkaConsumerGroupRepository.clear();
        reset(dataSubscriptionStateClient);
    }

    @Test
    @Disabled
    void testDataPortMass() throws InterruptedException {
        for (int i = 0; i < 2 * mass; i++) {
            kafkaTopicRepository.save(
                    KafkaTopic.builder().dataPortId("to-remove-abc-" + i).topicName("data-topicX-" + i).build());
        }

        for (int i = 0; i < mass; i++) {
            adminClient.createTopics(
                    Arrays.asList(
                            new NewTopic("data-topic1-" + i, 2, (short) 1),
                            new NewTopic("data-topic2-" + i, 3, (short) 1),
                            new NewTopic("data-topic3-" + i, 4, (short) 1)));

            schemaRegistryUtil.addSchemaVersion(
                    "data-topic1-" + i, KafkaStreamPart.VALUE, "registry/confluent/version1.json");
            schemaRegistryUtil.addSchemaVersion(
                    "data-topic1-" + i, KafkaStreamPart.KEY, "registry/confluent/version1.json");
            schemaRegistryUtil.addSchemaVersion(
                    "data-topic2-" + i, KafkaStreamPart.VALUE, "registry/confluent/version1.json");
            schemaRegistryUtil.addSchemaVersion(
                    "data-topic2-" + i, KafkaStreamPart.KEY, "registry/confluent/version1.json");
            schemaRegistryUtil.addSchemaVersion(
                    "data-topic3-" + i, KafkaStreamPart.VALUE, "registry/confluent/version1.json");
            schemaRegistryUtil.addSchemaVersion(
                    "data-topic3-" + i, KafkaStreamPart.KEY, "registry/confluent/version1.json");
        }

        await()
                .atMost(Duration.ofSeconds(60))
                .until(
                        () ->
                                (long) adminClient
                                        .listTopics()
                                        .names()
                                        .get()
                                        .size() >= 3 * mass);

        dataService.updateTopics();

        ArgumentCaptor<SaveDataPortRequest> captor = ArgumentCaptor.forClass(SaveDataPortRequest.class);
        verify(dataPortClient, timeout(5000).times(5 * mass)).save(captor.capture());
        // TODO here we should test memory usage etc.
    }


    @Test
    @Disabled
    void testDataSubscriptionState() throws InterruptedException {

        List<NewTopic> newTopicsList = new ArrayList<>();
        for (int i = 0; i < mass; i++) {
            kafkaTopicRepository.save(
                    KafkaTopic.builder().dataPortId("abc-" + i).topicName("data-topic1-" + i).build());
            kafkaConsumerGroupRepository.save(
                    KafkaConsumerGroup.builder()
                            .dataSubscriptionStateId("to-delete-subId-" + i)
                            .consumerGroupName("groupX-" + i)
                            .dataPortId("abc-" + i)
                            .topicName("data-topic1-" + i)
                            .build());

            newTopicsList.add(new NewTopic("data-topic1-" + i, 2, (short) 1));
            newTopicsList.add(new NewTopic("data-topic2-" + i, 3, (short) 1));
            newTopicsList.add(new NewTopic("data-topic3-" + i, 4, (short) 1));

        }
        adminClient.createTopics(newTopicsList).all();

        final byte[] data = "data".getBytes(StandardCharsets.UTF_8);

        for (int i = 0; i < mass; i++) {
            // produce some data
            Arrays.asList("data-topic1-" + i, "data-topic2-" + i, "data-topic3-" + i)
                    .forEach(
                            topic -> {
                                for (int j = 0; j < 10; j++) {
                                    kafkaUtils.produceAsync(topic, data);
                                }
                            });
        }
        dataService.updateConsumerGroups();
        // TODO here we should test memory usage etc.
    }
}
