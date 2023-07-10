package io.spoud.agoora.agents.kafka.decoder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.kafka.decoder.avro.SampleDecoderAvroConfluent;
import io.spoud.agoora.agents.kafka.decoder.json.SampleDecoderJson;
import io.spoud.agoora.agents.kafka.decoder.xml.SampleDecoderXml;
import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import io.spoud.agoora.agents.kafka.utils.ResourceUtil;
import io.spoud.agoora.agents.kafka.utils.SchemaRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class DecoderServiceTest {

  public static final String ISS_TOPIC_AVRO = "iss-topic-avro";
  public static final String ISS_TOPIC_JSON = "iss-topic-json";
  public static final String BERN_PARKING_TOPIC_XML = "bern-parking-topic-xml";

  @Inject DecoderService decoderService;
  @Inject ObjectMapper objectMapper;
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

    String jsonValue = ResourceUtil.getFile("data/json-iss.json");

    DecodedMessages decodedKey = decoderService.decodeKey(ISS_TOPIC_JSON, null);
    DecodedMessages decodedValue =
        decoderService.decodeValue(
            ISS_TOPIC_JSON, Arrays.asList(jsonValue.getBytes(StandardCharsets.UTF_8)));

    assertThat(decodedKey.getEncoding()).isEqualTo(DataEncoding.UNKNOWN);

    assertThat(decodedValue.getEncoding()).isEqualTo(DataEncoding.JSON);
    assertThat(decodedValue.getUtf8String().get(0)).isEqualTo(jsonValue);
  }

  @Test
  void testXml() throws JsonProcessingException {
    String xmlValue = ResourceUtil.getFile("data/xml-bern-parking.xml");
    String xmlToJsonValue = ResourceUtil.getFile("data/xml-bern-parking.json");
    DecodedMessages decodedKey = decoderService.decodeKey(BERN_PARKING_TOPIC_XML, null);
    DecodedMessages decodedValue =
        decoderService.decodeValue(
            BERN_PARKING_TOPIC_XML, Arrays.asList(xmlValue.getBytes(StandardCharsets.UTF_8)));

    assertThat(decodedKey.getEncoding()).isEqualTo(DataEncoding.UNKNOWN);

    assertThat(decodedValue.getEncoding()).isEqualTo(DataEncoding.XML);
    assertThat(objectMapper.readTree(decodedValue.getUtf8String().get(0))).isEqualTo(objectMapper.readTree(xmlToJsonValue));
  }

  @Test
  void testAvro() throws JsonProcessingException {
    String avroToJsonValue = ResourceUtil.getFile("data/avro-iss.json");

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
    assertThat(objectMapper.readTree(decodedValue.getUtf8String().get(0)))
        .isEqualTo(objectMapper.readTree(avroToJsonValue));
  }
}
