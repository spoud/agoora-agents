package io.spoud.agoora.agents.kafka.service;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.LookerClient;
import io.spoud.agoora.agents.api.client.ProfilerClient;
import io.spoud.agoora.agents.api.client.SchemaClient;
import io.spoud.agoora.agents.api.observers.ProfileResponseObserver;
import io.spoud.agoora.agents.kafka.data.KafkaTopic;
import io.spoud.agoora.agents.kafka.repository.KafkaTopicRepository;
import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import io.spoud.agoora.agents.kafka.utils.KafkaUtils;
import io.spoud.agoora.agents.kafka.utils.SchemaRegistryUtil;
import io.spoud.agoora.agents.test.mock.LookerClientMockProvider;
import io.spoud.agoora.agents.test.mock.ProfilerClientMockProvider;
import io.spoud.agoora.agents.test.mock.SchemaClientMockProvider;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.looker.domain.v1alpha1.DataProfilingError;
import io.spoud.sdm.looker.v1alpha1.AddDataProfileRequest;
import io.spoud.sdm.schema.domain.v1alpha.SchemaEncoding;
import io.spoud.sdm.schema.domain.v1alpha.SchemaSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
class ProfilerServiceTest {

  public static final String PROFILE_TOPIC_JSON = "profile-topic-json";
  public static final String PROFILE_TOPIC_SMALL = "profile-topic-small";
  public static final String PROFILE_TOPIC_NO_DATA = "profile-topic-no-data";
  public static final String PROFILE_TOPIC_AVRO = "profile-topic-avro";
  public static final String PROFILE_TOPIC_UNKNOWN_DATA = "profile-topic-wrong-data";
  public static final byte[] JSON_DATA = "{\"field\":\"hello\"}".getBytes(StandardCharsets.UTF_8);
  public static final String AVRO_DATA = "146f6270797068777769788e0428848a3c01";
  public static final byte[] UNKNOWN_DATA = new byte[]{(byte) 0xA1, (byte) 0xB2, (byte) 0xC3};

  @Inject
  SchemaClient schemaClient;
  @Inject
  ProfilerClient profilerClient;
  @Inject
  LookerClient lookerClient;
  @Inject
  ProfilerService profilerService;
  @Inject
  KafkaTopicRepository kafkaTopicRepository;
  @Inject
  KafkaUtils kafkaUtils;
  @Inject
  SchemaRegistryUtil schemaRegistryUtil;

  @BeforeEach
  void setup() {
    LookerClientMockProvider.defaultMock(lookerClient);
    ProfilerClientMockProvider.defaultMock(profilerClient);
    SchemaClientMockProvider.defaultMock(schemaClient);
  }

  @AfterEach
  void tearDown() {
    kafkaTopicRepository.clear();
  }

  @Test
  @Timeout(30)
  void testJsonProfiling() {
    kafkaTopicRepository.save(
        KafkaTopic.builder().topicName(PROFILE_TOPIC_JSON).dataPortId("hij").build());
    kafkaTopicRepository.save(
        KafkaTopic.builder().topicName("topic-without-dataport").build()); // should be ignored

    for (int i = 0; i < 15; i++) {
      kafkaUtils.produce(PROFILE_TOPIC_JSON, JSON_DATA);
    }

    profilerService.profileData();

    ArgumentCaptor<List<byte[]>> samplesCaptor = ArgumentCaptor.forClass(List.class);
    verify(profilerClient).profileData(eq(PROFILE_TOPIC_JSON), samplesCaptor.capture());
    assertThat(samplesCaptor.getAllValues()).hasSize(1);
    assertThat(samplesCaptor.getAllValues().get(0)).hasSize(10);

    verify(schemaClient)
        .saveSchema(
            eq(ResourceEntity.Type.DATA_PORT),
            eq("hij"),
            eq("/default/"),
            any(),
            eq(SchemaSource.Type.INFERRED),
            eq(SchemaEncoding.Type.JSON),
            eq(""),
            eq(SchemaEncoding.Type.UNKNOWN));
  }

  @Test
  @Timeout(30)
  void testAvroProfiling() {
    final Long id =
        schemaRegistryUtil
            .addSchemaVersion(
                PROFILE_TOPIC_AVRO, KafkaStreamPart.VALUE, "registry/confluent/randomv1.json")
            .getId();

    final byte[] avroBytes = schemaRegistryUtil.getAvroBytes(id, AVRO_DATA);

    kafkaTopicRepository.save(
        KafkaTopic.builder().topicName(PROFILE_TOPIC_AVRO).dataPortId("def").build());
    for (int i = 0; i < 15; i++) {
      kafkaUtils.produce(PROFILE_TOPIC_AVRO, avroBytes);
    }

    profilerService.profileData();

    ArgumentCaptor<List<byte[]>> samplesCaptor = ArgumentCaptor.forClass(List.class);
    verify(profilerClient).profileData(eq(PROFILE_TOPIC_AVRO), samplesCaptor.capture());
    assertThat(samplesCaptor.getAllValues()).hasSize(1);
    assertThat(samplesCaptor.getAllValues().get(0)).hasSize(10);

    verify(schemaClient)
        .saveSchema(
            eq(ResourceEntity.Type.DATA_PORT),
            eq("def"),
            eq("/default/"),
            any(),
            eq(SchemaSource.Type.INFERRED),
            eq(SchemaEncoding.Type.JSON),
            eq(""),
            eq(SchemaEncoding.Type.UNKNOWN));
  }

  @Test
  @Timeout(30)
  void testNotEnoughData() {
    kafkaTopicRepository.save(
        KafkaTopic.builder().topicName(PROFILE_TOPIC_SMALL).dataPortId("mno").build());
    for (int i = 0; i < 2; i++) {
      kafkaUtils.produce(PROFILE_TOPIC_SMALL, JSON_DATA);
    }

    profilerService.profileData();

    ArgumentCaptor<List<byte[]>> samplesCaptor = ArgumentCaptor.forClass(List.class);
    verify(profilerClient).profileData(eq(PROFILE_TOPIC_SMALL), samplesCaptor.capture());
    assertThat(samplesCaptor.getAllValues()).hasSize(1);
    assertThat(samplesCaptor.getAllValues().get(0)).hasSize(2);

    verify(schemaClient)
        .saveSchema(
            eq(ResourceEntity.Type.DATA_PORT),
            eq("mno"),
            eq("/default/"),
            any(),
            eq(SchemaSource.Type.INFERRED),
            eq(SchemaEncoding.Type.JSON),
            eq(""),
            eq(SchemaEncoding.Type.UNKNOWN));

    ArgumentCaptor<AddDataProfileRequest> captor =
        ArgumentCaptor.forClass(AddDataProfileRequest.class);
    verify(lookerClient).addDataProfile(captor.capture());
    assertThat(captor.getValue().getDataSamplesCount()).isEqualTo(2);
    assertThat(captor.getValue().hasError()).isFalse();
    assertThat(captor.getValue().getEntityRef()).isNotNull();
    assertThat(captor.getValue().getEntityRef().getId()).isEqualTo("mno");
  }

  @Test
  @Timeout(30)
  void testNoData() {
    reset(profilerClient);
    reset(schemaClient);
    kafkaTopicRepository.save(
        KafkaTopic.builder().topicName(PROFILE_TOPIC_NO_DATA).dataPortId("klm").build());

    profilerService.profileData();

    verify(schemaClient, never()).saveSchema(any(), eq("klm"), any(), any(), any(), any(), any(), any());
    verify(profilerClient, never()).profileData(eq(PROFILE_TOPIC_NO_DATA), any());

    ArgumentCaptor<AddDataProfileRequest> captor =
        ArgumentCaptor.forClass(AddDataProfileRequest.class);
    verify(lookerClient).addDataProfile(captor.capture());
    assertThat(captor.getValue().getDataSamplesCount()).isZero();
    assertThat(captor.getValue().hasError()).isTrue();
    assertThat(captor.getValue().getError().getType()).isEqualTo(DataProfilingError.Type.NO_DATA);
    assertThat(captor.getValue().getEntityRef()).isNotNull();
    assertThat(captor.getValue().getEntityRef().getId()).isEqualTo("klm");
  }

  @Test
  @Timeout(30)
  void testUnknownData() {
    kafkaTopicRepository.save(
        KafkaTopic.builder().topicName(PROFILE_TOPIC_UNKNOWN_DATA).dataPortId("pqr").build());
    for (int i = 0; i < 15; i++) {
      kafkaUtils.produce(PROFILE_TOPIC_UNKNOWN_DATA, UNKNOWN_DATA);
    }

    profilerService.profileData();

    verify(schemaClient, never()).saveSchema(any(), eq("pqr"), any(), any(), any(), any(), any(), any());
    verify(profilerClient, never()).profileData(eq(PROFILE_TOPIC_UNKNOWN_DATA), any());

    ArgumentCaptor<AddDataProfileRequest> captor =
        ArgumentCaptor.forClass(AddDataProfileRequest.class);
    verify(lookerClient).addDataProfile(captor.capture());
    assertThat(captor.getValue().getDataSamplesCount()).isEqualTo(0);
    assertThat(captor.getValue().hasError()).isTrue();
    assertThat(captor.getValue().getError().getType())
        .isEqualTo(DataProfilingError.Type.UNKNOWN_ENCODING);
    assertThat(captor.getValue().getError().getMessage())
        .isEqualTo("No decoder found for the value of topic 'profile-topic-wrong-data'");
    assertThat(captor.getValue().getEntityRef()).isNotNull();
    assertThat(captor.getValue().getEntityRef().getId()).isEqualTo("pqr");
  }

  @Test
  void testSchema() {
    final KafkaTopic kafkaTopic = KafkaTopic.builder().dataPortId("stu").build();
    profilerService.uploadSchema(
        kafkaTopic, ProfileResponseObserver.ProfilerResponse.builder().build());

    verifyNoMoreInteractions(schemaClient);

    profilerService.uploadSchema(kafkaTopic,
        ProfileResponseObserver.ProfilerResponse.builder().schema("blabla").build());

    verify(schemaClient)
        .saveSchema(
            eq(ResourceEntity.Type.DATA_PORT),
            eq("stu"),
            eq("/default/"),
            eq("blabla"),
            eq(SchemaSource.Type.INFERRED),
            eq(SchemaEncoding.Type.JSON),
            eq(""),
            eq(SchemaEncoding.Type.UNKNOWN));
  }
}
