package io.spoud.agoora.agents.kafka.decoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import io.spoud.agoora.agents.kafka.schema.confluent.ConfluentRegistrySubjectResource;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class DecoderServiceTest {

  public static final String ISS_TOPIC_AVRO = "iss-topic-avro";
  public static final String ISS_TOPIC_JSON = "iss-topic-json";
  public static final String ISS_JSON_VALUE =
      "{\"iss_position\": {\"longitude\": \"-113.0217\", \"latitude\": \"47.4961\"}, \"message\": \"success\", \"timestamp\": 1584691915}";

  private ObjectMapper objectMapper = new ObjectMapper();

  private String valueSchemaAsString;

  @Inject private DecoderService decoderService;

  @Inject @RestClient private ConfluentRegistrySubjectResource confluentRegistrySubjectResource;

  @BeforeEach
  public void setUp() throws IOException {

    addSchemaVersion(ISS_TOPIC_AVRO, KafkaStreamPart.VALUE, "registry/confluent/iss.json");

    //        decoderService = new DecoderService(Arrays.asList(
    //                new SampleDecoderJson(objectMapper),
    //                new SampleDecoderAvroConfluent(objectMapper, config)
    //        ));

    //        this.valueSchemaAsString =
    //                objectMapper.writeValueAsString(
    //                        SchemaRegistrySubject.builder()
    //                                .id(1)
    //                                .version(2)
    //                                .subject("value-subject")
    //                                .schema(getIssAvroSchema())
    //                                .build());
  }

  @AfterEach
  public void tearDown() {}

  @Test
  public void testJson() throws URISyntaxException, org.apache.commons.codec.DecoderException {
    //        mockServer
    //                .expect(requestTo(new URI("http://mock:8081/subjects")))
    //                .andExpect(method(HttpMethod.GET))
    //                .andRespond(
    //                        withStatus(HttpStatus.OK)
    //                                .contentType(MediaType.APPLICATION_JSON)
    //                                .body("[\""+ISS_TOPIC_AVRO+"-value\"]"));

    DecodedMessage decodedKey = decoderService.decodeKey(ISS_TOPIC_JSON, null);
    DecodedMessage decodedValue = decoderService.decodeValue(ISS_TOPIC_JSON, getJsonValue());

    assertThat(decodedKey).isNull();

    assertThat(decodedValue.getEncoding()).isEqualTo(DataEncoding.JSON);
    assertThat(decodedValue.getDecodedValue()).isEqualTo(ISS_JSON_VALUE);
  }

  @Test
  public void testAvro() throws URISyntaxException, org.apache.commons.codec.DecoderException {
    //        mockServer
    //                .expect(requestTo(new URI("http://mock:8081/subjects")))
    //                .andExpect(method(HttpMethod.GET))
    //                .andRespond(
    //                        withStatus(HttpStatus.OK)
    //                                .contentType(MediaType.APPLICATION_JSON)
    //                                .body("[\""+ISS_TOPIC_AVRO+"-value\"]"));
    //
    //        mockServer
    //                .expect(requestTo(new URI("http://mock:8081/schemas/ids/3")))
    //                .andExpect(method(HttpMethod.GET))
    //                .andRespond(
    //                        withStatus(HttpStatus.OK)
    //                                .contentType(MediaType.APPLICATION_JSON)
    //                                .body(valueSchemaAsString));

    DecodedMessage decodedKey = decoderService.decodeKey(ISS_TOPIC_AVRO, null);
    DecodedMessage decodedValue = decoderService.decodeValue(ISS_TOPIC_AVRO, getIssAvroValue());

    assertThat(decodedKey).isNull();

    assertThat(decodedValue.getEncoding()).isEqualTo(DataEncoding.AVRO);
    assertThat(decodedValue.getDecodedValue()).isEqualTo(ISS_JSON_VALUE);
  }

  private byte[] getIssAvroValue() throws org.apache.commons.codec.DecoderException {
    String hex = "0000000003122d3131332e303231370e34372e34393631000e7375636365737396eba3e70b";
    return Hex.decodeHex(hex);
  }

  private byte[] getJsonValue() throws DecoderException {
    return ISS_JSON_VALUE.getBytes(StandardCharsets.UTF_8);
  }
  //
  //    private String getIssAvroSchema() {
  //        return "{\n"
  //                + "  \"type\": \"record\",\n"
  //                + "  \"name\": \"IssPositionMessage\",\n"
  //                + "  \"namespace\": \"data.producer.iss\",\n"
  //                + "  \"fields\": [\n"
  //                + "    {\n"
  //                + "      \"name\": \"iss_position\",\n"
  //                + "      \"type\": {\n"
  //                + "        \"type\": \"record\",\n"
  //                + "        \"name\": \"IssPosition\",\n"
  //                + "        \"fields\": [\n"
  //                + "          {\n"
  //                + "            \"name\": \"longitude\",\n"
  //                + "            \"type\": \"string\"\n"
  //                + "          },\n"
  //                + "          {\n"
  //                + "            \"name\": \"latitude\",\n"
  //                + "            \"type\": \"string\"\n"
  //                + "          }\n"
  //                + "        ]\n"
  //                + "      }\n"
  //                + "    },\n"
  //                + "    {\n"
  //                + "      \"name\": \"message\",\n"
  //                + "      \"type\": [\n"
  //                + "        \"string\",\n"
  //                + "        \"null\"\n"
  //                + "      ]\n"
  //                + "    },\n"
  //                + "    {\n"
  //                + "      \"name\": \"timestamp\",\n"
  //                + "      \"type\": \"long\"\n"
  //                + "    }\n"
  //                + "  ]\n"
  //                + "}";
  //    }

  private void addSchemaVersion(String topic, KafkaStreamPart part, String file)
      throws IOException {
    String content =
        IOUtils.toString(
            DecoderServiceTest.class.getClassLoader().getResourceAsStream(file), "UTF-8");
    confluentRegistrySubjectResource.addNewSchemaVersion(topic, part, content);
  }
}
