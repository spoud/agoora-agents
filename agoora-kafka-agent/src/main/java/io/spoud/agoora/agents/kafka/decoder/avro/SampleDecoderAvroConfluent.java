package io.spoud.agoora.agents.kafka.decoder.avro;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.spoud.agoora.agents.kafka.decoder.DataEncoding;
import io.spoud.agoora.agents.kafka.decoder.DecodedMessages;
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

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class SampleDecoderAvroConfluent implements SampleDecoder {

  // https://docs.confluent.io/current/schema-registry/serializer-formatter.html#wire-format
  private static final int CONFLUENT_WRAPPER_SIZE = 5;
  private static final byte CONFLUENT_MAGIC_BYTE = 0;

  private final ConfluentSchemaRegistry confluentSchemaRegistry;
  private final Cache<Long, Schema> schemaCacheById =
      Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(Duration.ofHours(1)).build();

  @Override
  public int getPriority() {
    return 20;
  }

  @Override
  public Optional<DecodedMessages> decode(
      String topic, KafkaStreamPart part, List<byte[]> dataList) {
    if (!eligible(dataList)) {
      LOG.trace("topic '{}' and part '{}' not eligible", topic, part);
      return Optional.empty();
    }

    final DataEncoding dataEncoding = DataEncoding.AVRO;

    return Optional.of(
            DecodedMessages.builder()
                .encoding(dataEncoding)
                .messages(
                    dataList.stream()
                        .map(
                            data -> {
                              long schemaId = getSchemaIdFromBytes(data);
                              return getCachedSchema(schemaId, topic, part)
                                  .map(raw -> decodeAvro(data, raw));
                            })
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList()))
                .build())
        .filter(msgs -> !msgs.getMessages().isEmpty());
  }

  protected byte[] decodeAvro(byte[] data, Schema schema) {
    try {
      DatumReader datumReader = new GenericDatumReader(schema);
      Decoder decoder =
          DecoderFactory.get()
              .binaryDecoder(
                  data, CONFLUENT_WRAPPER_SIZE, data.length - CONFLUENT_WRAPPER_SIZE, null);
      Object avroDatum = datumReader.read(null, decoder);
      return avroDatum.toString().getBytes(StandardCharsets.UTF_8);
    } catch (AvroRuntimeException ex) {
      LOG.error("Unable to decode avro message, enable DEBUG to see the message", ex);
      LOG.debug("Unable to decode avro message: '{}'", Hex.encodeHexString(data));
    } catch (IOException e) {
      LOG.error("Unable to deserialize object", e);
    }
    return null;
  }

  protected long getSchemaIdFromBytes(byte[] data) {
    ByteBuffer buffer = ByteBuffer.wrap(data);
    int signedInt = buffer.getInt(1);
    long unsignedLong = Integer.toUnsignedLong(signedInt);
    return unsignedLong;
  }

  protected Optional<Schema> parseAvroSchema(String schemaStr) {
    try {
      return Optional.of(new Schema.Parser().parse(schemaStr));
    } catch (SchemaParseException ex) {
      LOG.error("Unable to parse avro schema: '{}'", schemaStr, ex);
    }
    return Optional.empty();
  }

  private boolean eligible(List<byte[]> data) {
    // must start with the magic byte + have the minimum header
    return data.stream()
        .allMatch(d -> d.length >= CONFLUENT_WRAPPER_SIZE && d[0] == CONFLUENT_MAGIC_BYTE);
  }

  private Optional<Schema> getCachedSchema(long schemaId, String topic, KafkaStreamPart part) {
    return Optional.ofNullable(
        schemaCacheById.get(
            schemaId,
            id ->
                confluentSchemaRegistry
                    .getSchemaById(schemaId)
                    .flatMap(
                        schema -> {
                          if (schema.getSchemaType() == null
                              || schema.getSchemaType().equals("AVRO")) {
                            return parseAvroSchema(schema.getSchema());
                          } else {
                            LOG.warn(
                                "Unsupported schema type '{}' for topic {} and part {}",
                                schema.getSchemaType(),
                                topic,
                                part);
                            return Optional.empty();
                          }
                        })
                    .orElse(null)));
  }
}
