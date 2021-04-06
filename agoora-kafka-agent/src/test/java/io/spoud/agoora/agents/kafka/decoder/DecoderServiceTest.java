package io.spoud.agoora.agents.kafka.decoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import io.spoud.agoora.agents.kafka.utils.SchemaRegistryUtil;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class DecoderServiceTest {

  public static final String ISS_TOPIC_AVRO = "iss-topic-avro";
  public static final String ISS_TOPIC_JSON = "iss-topic-json";
  public static final String ISS_JSON_VALUE =
      "{\"iss_position\": {\"longitude\": \"-113.0217\", \"latitude\": \"47.4961\"}, \"message\": \"success\", \"timestamp\": 1584691915}";

  private ObjectMapper objectMapper = new ObjectMapper();

  @Inject private DecoderService decoderService;

  @Inject private SchemaRegistryUtil schemaRegistryUtil;

  @Test
  public void testJson() {

    DecodedMessage decodedKey = decoderService.decodeKey(ISS_TOPIC_JSON, null);
    DecodedMessage decodedValue = decoderService.decodeValue(ISS_TOPIC_JSON, getJsonValue());

    assertThat(decodedKey.getEncoding()).isEqualTo(DataEncoding.UNKNOWN);

    assertThat(decodedValue.getEncoding()).isEqualTo(DataEncoding.JSON);
    assertThat(decodedValue.getUtf8String()).isEqualTo(ISS_JSON_VALUE);
  }

  @Test
  public void testAvro() throws org.apache.commons.codec.DecoderException, IOException {
    final long id =
        schemaRegistryUtil
            .addSchemaVersion(ISS_TOPIC_AVRO, KafkaStreamPart.VALUE, "registry/confluent/iss.json")
            .getId();

    DecodedMessage decodedKey = decoderService.decodeKey(ISS_TOPIC_AVRO, null);
    DecodedMessage decodedValue =
        decoderService.decodeValue(
            ISS_TOPIC_AVRO,
            schemaRegistryUtil.getAvroBytes(
                id, "122d3131332e303231370e34372e34393631000e7375636365737396eba3e70b"));

    assertThat(decodedKey.getEncoding()).isEqualTo(DataEncoding.UNKNOWN);

    assertThat(decodedValue.getEncoding()).isEqualTo(DataEncoding.AVRO);
    assertThat(decodedValue.getUtf8String()).isEqualTo(ISS_JSON_VALUE);
  }

  //  private byte[] getIssAvroValue(int id) throws org.apache.commons.codec.DecoderException {
  //    String hex =
  //        "00"
  //            + String.format("%08d", id)
  //            + ;
  //    return Hex.decodeHex(hex);
  //  }

  private byte[] getJsonValue() {
    return ISS_JSON_VALUE.getBytes(StandardCharsets.UTF_8);
  }
}
