package io.spoud.agoora.agents.kafka.decoder;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.kafka.decoder.avro.SampleDecoderAvroConfluent;
import io.spoud.agoora.agents.kafka.decoder.json.SampleDecoderJson;
import io.spoud.agoora.agents.kafka.decoder.xml.SampleDecoderXml;
import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import io.spoud.agoora.agents.kafka.utils.SchemaRegistryUtil;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class DecoderServiceTest {

  public static final String ISS_TOPIC_AVRO = "iss-topic-avro";
  public static final String ISS_TOPIC_JSON = "iss-topic-json";
  public static final String ISS_JSON_VALUE =
      "{\"iss_position\": {\"longitude\": \"-113.0217\", \"latitude\": \"47.4961\"}, \"message\": \"success\", \"timestamp\": 1584691915}";

  @Inject DecoderService decoderService;

  @Inject SchemaRegistryUtil schemaRegistryUtil;

  @Test
  void testDecoderOrder() {
    final List<SampleDecoder> decoders = decoderService.getSampleDecoders();
    assertThat(decoders).hasSize(3);
    assertThat(decoders.get(0)).isInstanceOf(SampleDecoderAvroConfluent.class);
    assertThat(decoders.get(1)).isInstanceOf(SampleDecoderXml.class);
    assertThat(decoders.get(2)).isInstanceOf(SampleDecoderJson.class);
  }

  @Test
  void testJson() {

    DecodedMessages decodedKey = decoderService.decodeKey(ISS_TOPIC_JSON, null);
    DecodedMessages decodedValue =
        decoderService.decodeValue(ISS_TOPIC_JSON, Arrays.asList(getJsonValue()));

    assertThat(decodedKey.getEncoding()).isEqualTo(DataEncoding.UNKNOWN);

    assertThat(decodedValue.getEncoding()).isEqualTo(DataEncoding.JSON);
    assertThat(decodedValue.getUtf8String().get(0)).isEqualTo(ISS_JSON_VALUE);
  }

  @Test
  void testAvro() {
    final long id =
        schemaRegistryUtil
            .addSchemaVersion(ISS_TOPIC_AVRO, KafkaStreamPart.VALUE, "registry/confluent/iss.json")
            .getId();

    DecodedMessages decodedKey = decoderService.decodeKey(ISS_TOPIC_AVRO, null);
    DecodedMessages decodedValue =
        decoderService.decodeValue(
            ISS_TOPIC_AVRO,
            Arrays.asList(
                schemaRegistryUtil.getAvroBytes(
                    id, "122d3131332e303231370e34372e34393631000e7375636365737396eba3e70b")));

    assertThat(decodedKey.getEncoding()).isEqualTo(DataEncoding.UNKNOWN);

    assertThat(decodedValue.getEncoding()).isEqualTo(DataEncoding.AVRO);
    assertThat(decodedValue.getUtf8String().get(0)).isEqualTo(ISS_JSON_VALUE);
  }

  private byte[] getJsonValue() {
    return ISS_JSON_VALUE.getBytes(StandardCharsets.UTF_8);
  }
}
