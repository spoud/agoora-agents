package io.spoud.agoora.agents.profiler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.spoud.sdm.profiler.domain.v1alpha1.ProfilerError;
import io.spoud.sdm.profiler.service.v1alpha1.MutinyProfilerGrpc;
import io.spoud.sdm.profiler.service.v1alpha1.ProfileDataStreamResponse;
import io.spoud.sdm.profiler.service.v1alpha1.ProfileRequest;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class ProfilerGrpcServiceTest {

    private ManagedChannel channel;
    private MutinyProfilerGrpc.MutinyProfilerStub stub;

    @BeforeEach
    void setup() {
        int port = ConfigProvider.getConfig()
                .getValue("quarkus.grpc.server.port", Integer.class);
        channel = ManagedChannelBuilder.forAddress("localhost", port)
                .usePlaintext()
                .build();
        stub = MutinyProfilerGrpc.newMutinyStub(channel);
    }

    @AfterEach
    void teardown() throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void testNoData() {
        List<ProfileDataStreamResponse> responses = stub
                .profileDataStream(Multi.createFrom().empty())
                .collect().asList()
                .await().indefinitely();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).hasMeta()).isTrue();
        assertThat(responses.get(0).getMeta().getError().getType())
                .isEqualTo(ProfilerError.Type.NO_DATA);
    }

    @Test
    void testInvalidJson() {
        List<ProfileDataStreamResponse> responses = stub
                .profileDataStream(Multi.createFrom().item(profileRequest("r0", "not-valid-json{{")))
                .collect().asList()
                .await().indefinitely();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getMeta().getError().getType())
                .isEqualTo(ProfilerError.Type.UNKNOWN_ENCODING);
    }

    @Test
    void testValidData() throws Exception {
        List<ProfileRequest> reqs = List.of(
                profileRequest("r1", "{\"name\":\"Alice\",\"age\":30}"),
                profileRequest("r1", "{\"name\":\"Bob\",\"age\":25}"),
                profileRequest("r1", "{\"name\":\"Carol\",\"age\":35}")
        );

        List<ProfileDataStreamResponse> responses = stub
                .profileDataStream(Multi.createFrom().iterable(reqs))
                .collect().asList()
                .await().indefinitely();

        assertThat(responses).hasSizeGreaterThanOrEqualTo(2);

        ProfileDataStreamResponse metaMsg = responses.get(0);
        assertThat(metaMsg.hasMeta()).isTrue();
        assertThat(metaMsg.getMeta().getError().getType()).isEqualTo(ProfilerError.Type.UNKNOWN);
        assertThat(metaMsg.getMeta().getTotalRecords()).isEqualTo(3);
        assertThat(metaMsg.getMeta().getSchema()).contains("properties");

        // Collect all profile chunks and verify JSON structure
        StringBuilder profileJson = new StringBuilder();
        for (int i = 1; i < responses.size(); i++) {
            assertThat(responses.get(i).hasProfile()).isTrue();
            profileJson.append(responses.get(i).getProfile());
        }

        JsonNode root = new ObjectMapper().readTree(profileJson.toString());
        assertThat(root.get("version").asText()).isEqualTo("3");
        assertThat(root.get("totalRecords").asInt()).isEqualTo(3);
        assertThat(root.get("columns").isArray()).isTrue();
        assertThat(root.get("columns").size()).isEqualTo(2); // name, age

        // Numeric column (age) should have histogram and percentiles
        JsonNode ageCol = findColumn(root, "age");
        assertThat(ageCol).isNotNull();
        assertThat(ageCol.get("min").asDouble()).isEqualTo(25.0);
        assertThat(ageCol.get("max").asDouble()).isEqualTo(35.0);
        assertThat(ageCol.has("histogram")).isTrue();
        assertThat(ageCol.has("percentiles")).isTrue();
    }

    @Test
    void testNestedJsonFlattening() throws Exception {
        List<ProfileRequest> reqs = List.of(
                profileRequest("r2", "{\"user\":{\"name\":\"Alice\",\"city\":\"Zurich\"},\"score\":10}"),
                profileRequest("r2", "{\"user\":{\"name\":\"Bob\",\"city\":\"Bern\"},\"score\":20}")
        );

        List<ProfileDataStreamResponse> responses = stub
                .profileDataStream(Multi.createFrom().iterable(reqs))
                .collect().asList()
                .await().indefinitely();

        assertThat(responses).hasSizeGreaterThanOrEqualTo(1);
        String schema = responses.get(0).getMeta().getSchema();
        assertThat(schema).contains("user/name");
        assertThat(schema).contains("user/city");

        // Verify flattened columns appear in profile JSON
        StringBuilder profileJson = new StringBuilder();
        for (int i = 1; i < responses.size(); i++) {
            profileJson.append(responses.get(i).getProfile());
        }
        JsonNode root = new ObjectMapper().readTree(profileJson.toString());
        assertThat(findColumn(root, "user/name")).isNotNull();
        assertThat(findColumn(root, "user/city")).isNotNull();
    }

    @Test
    void testRequestIdIsEchoed() {
        List<ProfileDataStreamResponse> responses = stub
                .profileDataStream(Multi.createFrom().item(profileRequest("my-request-id-123", "{\"x\":1}")))
                .collect().asList()
                .await().indefinitely();

        assertThat(responses.get(0).getMeta().getRequestId()).isEqualTo("my-request-id-123");
    }

    private JsonNode findColumn(JsonNode root, String name) {
        for (JsonNode col : root.get("columns")) {
            if (name.equals(col.get("name").asText())) return col;
        }
        return null;
    }

    private ProfileRequest profileRequest(String requestId, String jsonData) {
        return ProfileRequest.newBuilder()
                .setRequestId(requestId)
                .setJsonData(jsonData)
                .build();
    }
}
