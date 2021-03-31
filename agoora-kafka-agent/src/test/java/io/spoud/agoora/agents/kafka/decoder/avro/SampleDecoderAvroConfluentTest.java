package io.spoud.agoora.agents.kafka.decoder.avro;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.kafka.AbstractService;
import io.spoud.agoora.agents.kafka.decoder.DataEncoding;
import io.spoud.agoora.agents.kafka.decoder.DecodedMessage;
import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import io.spoud.agoora.agents.kafka.utils.SchemaRegistryUtil;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SampleDecoderAvroConfluentTest extends AbstractService {

  @Inject private SampleDecoderAvroConfluent sampleDecoderConfluent;

  @Inject private SchemaRegistryUtil schemaRegistryUtil;

  @Test
  public void decodeSchemaId() {
    assertThat(sampleDecoderConfluent.getSchemaIdFromBytes(new byte[] {0x0, 0x0, 0x0, 0x0, 0x0}))
        .isEqualTo(0L);
    assertThat(
            sampleDecoderConfluent.getSchemaIdFromBytes(
                new byte[] {0x0, 0x0, 0x0, 0x0, (byte) 0xff}))
        .isEqualTo(255L);
    assertThat(sampleDecoderConfluent.getSchemaIdFromBytes(new byte[] {0x0, 0x0, 0x0, 0x0, 0x1}))
        .isEqualTo(1L);
    assertThat(sampleDecoderConfluent.getSchemaIdFromBytes(new byte[] {0x0, 0x0, 0x0, 0x1, 0x0}))
        .isEqualTo(256L);
    assertThat(sampleDecoderConfluent.getSchemaIdFromBytes(new byte[] {0x0, 0x0, 0x1, 0x0, 0x0}))
        .isEqualTo(65536L);
    assertThat(sampleDecoderConfluent.getSchemaIdFromBytes(new byte[] {0x0, 0x1, 0x0, 0x0, 0x0}))
        .isEqualTo(16777216L);
    assertThat(sampleDecoderConfluent.getSchemaIdFromBytes(new byte[] {0x0, 0x1, 0x2, 0x3, 0x4}))
        .isEqualTo(16909060L);
    assertThat(
            sampleDecoderConfluent.getSchemaIdFromBytes(
                new byte[] {0x0, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff}))
        .isEqualTo(4294967295L);
  }

  @Test
  public void testEncodingAndDecoding() throws IOException {
    Schema schema = schemaRegistryUtil.getSchemaFromFile("registry/confluent/randomv1.json");
    DatumWriter writer = new GenericDatumWriter<>(schema);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(new byte[] {0, 0, 0, 0, 1}); // confluent magic bytes
    BinaryEncoder encoder = EncoderFactory.get().blockingBinaryEncoder(out, null);
    writer.write(getRandomRecord(schema), encoder);
    encoder.flush();
    byte[] bytes = out.toByteArray();

    String decode =
        new String(sampleDecoderConfluent.decode(bytes, schema), StandardCharsets.UTF_8);
    assertThat(decode).isNotNull();
    assertThat(decode)
        .isEqualTo(
            "{\"random_string\": \"hello\", \"random_integer\": 1, \"random_float\": 1.5, \"random_boolean\": true}");
  }

  @Test
  public void testDecodingReadData() throws DecoderException {
    Schema schema = schemaRegistryUtil.getSchemaFromFile("registry/confluent/randomv1.json");
    String hexData = "0000000001146f6270797068777769788e0428848a3c01";
    byte[] bytes = Hex.decodeHex(hexData);

    String decode =
        new String(sampleDecoderConfluent.decode(bytes, schema), StandardCharsets.UTF_8);

    assertThat(decode)
        .isEqualTo(
            "{\"random_string\": \"obpyphwwix\", \"random_integer\": 263, \"random_float\": 0.01690872, \"random_boolean\": true}");
  }

  @Test
  public void testDecodingEvolutionData() {
    final long v1 =
        schemaRegistryUtil
            .addSchemaVersion("avro-topic1", KafkaStreamPart.VALUE, "registry/confluent/randomv1.json")
            .getId();
    final long v2 =
        schemaRegistryUtil
            .addSchemaVersion("avro-topic1", KafkaStreamPart.VALUE, "registry/confluent/randomv2.json")
            .getId();

    byte[] bytes = schemaRegistryUtil.getAvroBytes(v1, "146f6270797068777769788e0428848a3c01");
    Optional<DecodedMessage> decode =
        sampleDecoderConfluent.decode("avro-topic1", KafkaStreamPart.VALUE, bytes);
    assertThat(decode).isPresent();
    assertThat(decode.get().getEncoding()).isEqualTo(DataEncoding.AVRO);
    assertThat(decode.get().getUtf8String())
        .isEqualTo(
            "{\"random_string\": \"obpyphwwix\", \"random_integer\": 263, \"random_float\": 0.01690872, \"random_boolean\": true}");

    byte[] bytes2 = schemaRegistryUtil.getAvroBytes(v2, "146f6270797068777769788e0428848a3c0106");
    Optional<DecodedMessage> decode2 =
        sampleDecoderConfluent.decode("avro-topic1", KafkaStreamPart.VALUE, bytes2);
    assertThat(decode2).isPresent();
    assertThat(decode2.get().getEncoding()).isEqualTo(DataEncoding.AVRO);
    assertThat(decode2.get().getUtf8String())
        .isEqualTo(
            "{\"random_string\": \"obpyphwwix\", \"random_integer\": 263, \"random_float\": 0.01690872, \"random_boolean\": true, \"d\": 3}");
  }

  @Test
  void testParseWrongSchema() {
    assertThat(sampleDecoderConfluent.parseSchema("blababa")).isEmpty();
  }

  @Test
  void testDecodeWrongData() {
    final Schema schema = schemaRegistryUtil.getSchemaFromFile("registry/confluent/randomv1.json");
    final byte[] bytes = schemaRegistryUtil.getAvroBytes(1, "4654654986416544");
    assertThat(sampleDecoderConfluent.decode(bytes, schema)).isNull();
  }

  public GenericRecord getRandomRecord(Schema schema) {
    GenericRecord record = new GenericData.Record(schema);
    record.put("random_string", "hello");
    record.put("random_integer", 1);
    record.put("random_float", 1.5f);
    record.put("random_boolean", true);
    return record;
  }
}
