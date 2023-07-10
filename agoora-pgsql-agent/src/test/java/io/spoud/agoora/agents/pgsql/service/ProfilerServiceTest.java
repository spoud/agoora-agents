package io.spoud.agoora.agents.pgsql.service;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.api.client.BlobClient;
import io.spoud.agoora.agents.api.client.LookerClient;
import io.spoud.agoora.agents.api.client.ProfilerClient;
import io.spoud.agoora.agents.test.mock.BlobClientMockProvider;
import io.spoud.agoora.agents.test.mock.LookerClientMockProvider;
import io.spoud.agoora.agents.test.mock.ProfilerClientMockProvider;
import io.spoud.agoora.agents.pgsql.repository.DataItemRepository;
import io.spoud.sdm.global.domain.v1.ResourceEntity;
import io.spoud.sdm.logistics.domain.v1.DataItem;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;

@QuarkusTest()
class ProfilerServiceTest {

  ArgumentCaptor<List<byte[]>> samplesCaptor = ArgumentCaptor.forClass(List.class);
  @Inject ProfilerService profilerService;
  @Inject DataItemRepository dataItemRepository;
  @Inject BlobClient blobClient;
  @Inject ProfilerClient profilerClient;
  @Inject LookerClient lookerClient;
  ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setup() {
    BlobClientMockProvider.defaultMock(blobClient);
    ProfilerClientMockProvider.defaultMock(profilerClient);
    LookerClientMockProvider.defaultMock(lookerClient);
    clearInvocations(lookerClient);
  }

  @AfterEach
  void tearDown() {
    dataItemRepository.clear();
  }

  @Test
  void testProfiler() {
    dataItemRepository.update(
        DataItem.newBuilder().setId("dataPortId1").setTransportUrl("t_city").build());

    profilerService.runProfiler();

    // wait for update dataProfile
    await()
        .atMost(Duration.ofSeconds(5))
        .until(() -> mockingDetails(lookerClient).getInvocations().size() >= 1);

    verify(profilerClient)
        .profileData(eq("t_city?profileJob=dataPortId1"), samplesCaptor.capture());

    List<Map<String, Object>> samples =
        samplesCaptor.getValue().stream().map(this::getFromJsonBytes).collect(Collectors.toList());
    assertThat(samples).hasSize(3);
    assertThat(samples.get(0))
        .containsAllEntriesOf(
            Map.of(
                "city_uuid", "7bb2fdb0-8f05-44e8-b062-8a7d94d83b47",
                "label", "Bern",
                "meta", "{\"strange_language\":true}",
                "created_by", "script",
                "updated_by", "script"));

    verify(blobClient)
        .uploadBlobUtf8(eq("<html/>"), eq("/default/"), eq(ResourceEntity.Type.DATA_ITEM));
  }

  @SneakyThrows
  public Map<String, Object> getFromJsonBytes(byte[] bytes) {
    return objectMapper.readValue(bytes, Map.class);
  }
}
