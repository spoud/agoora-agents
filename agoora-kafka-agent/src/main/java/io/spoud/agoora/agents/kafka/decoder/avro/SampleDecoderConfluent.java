package io.spoud.agoora.agents.kafka.decoder.avro;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.spoud.agoora.agents.kafka.decoder.DataEncoding;
import io.spoud.agoora.agents.kafka.decoder.DecodedMessage;
import io.spoud.agoora.agents.kafka.decoder.SampleDecoder;
import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import io.spoud.agoora.agents.kafka.schema.confluent.ConfluentSchemaRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.SchemaParseException;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class SampleDecoderConfluent implements SampleDecoder {

  // https://docs.confluent.io/current/schema-registry/serializer-formatter.html#wire-format
  private static final int CONFLUENT_WRAPPER_SIZE = 5;
  private static final byte CONFLUENT_MAGIC_BYTE = 0;

  private final ObjectMapper objectMapper;
  private final ConfluentSchemaRegistry confluentSchemaRegistry;

  @Override
  public int getPriority() {
    return 20;
  }

  @Override
  public Optional<DecodedMessage> decode(String topic, KafkaStreamPart part, byte[] data) {
    if (data.length < CONFLUENT_WRAPPER_SIZE) {
      LOG.debug("Wrong avro content. Payload must be at least {} bytes", CONFLUENT_WRAPPER_SIZE);
      return Optional.empty();
    }
    if (data[0] != CONFLUENT_MAGIC_BYTE) {
      LOG.debug("Wrong magic byte for topic '{}' and part '{}'", topic, part);
      return Optional.empty();
    }
    long schemaId = getSchemaIdFromBytes(data);

    return confluentSchemaRegistry
        .getSchemaById(schemaId)
        .flatMap(this::parseSchema)
        .map(schema -> decode(data, schema))
        .map(
            decoded ->
                DecodedMessage.builder()
                    .decodedString(decoded)
                    .encoding(DataEncoding.AVRO)
                    .build());
  }

  protected String decode(byte[] data, Schema schema) {
    try {
      DatumReader datumReader = new GenericDatumReader(schema);
      Decoder decoder =
          DecoderFactory.get()
              .binaryDecoder(
                  data, CONFLUENT_WRAPPER_SIZE, data.length - CONFLUENT_WRAPPER_SIZE, null);
      Object avroDatum = datumReader.read(null, decoder);
      return avroDatum.toString();
    } catch (AvroRuntimeException ex) {
      LOG.error("Unable to decode avro message, enable DEBUG to see the message", ex);
      LOG.debug("Unable to decode avro message: '{}'", Hex.encodeHexString(data));
    } catch (IOException e) {
      LOG.error("Unable to deserialize object ", e);
    }
    return null;
  }

  protected long getSchemaIdFromBytes(byte[] data) {
    ByteBuffer buffer = ByteBuffer.wrap(data);
    int signedInt = Integer.valueOf(buffer.getInt(1));
    long unsignedLong = Integer.toUnsignedLong(signedInt);
    return unsignedLong;
  }

  protected Optional<Schema> parseSchema(String schemaStr) {
    try {
      return Optional.of(new Schema.Parser().parse(schemaStr));
    } catch (SchemaParseException ex) {
      LOG.error("Unable to parse schema: '{}'", schemaStr, ex);
    }
    return Optional.empty();
  }
}
