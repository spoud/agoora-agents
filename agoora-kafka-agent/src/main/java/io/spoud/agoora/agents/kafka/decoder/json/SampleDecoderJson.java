package io.spoud.agoora.agents.kafka.decoder.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spoud.agoora.agents.kafka.decoder.DataEncoding;
import io.spoud.agoora.agents.kafka.decoder.DecodedMessage;
import io.spoud.agoora.agents.kafka.decoder.DecoderException;
import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.spoud.agoora.agents.kafka.decoder.SampleDecoder;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class SampleDecoderJson implements SampleDecoder {

  private final ObjectMapper objectMapper;

  @Override
  public int getPriority() {
    return 10;
  }

  @Override
  public Optional<DecodedMessage> decode(
          String topic, KafkaStreamPart kafkaStreamPart, byte[] data) {

    try {
      String decodedString = new String(data, StandardCharsets.UTF_8);

      if (decodedString.equalsIgnoreCase("null")) {
        throw new DecoderException(DecoderException.DecoderExceptionType.NULL);
      }
      if (decodedString.isBlank()) {
        throw new DecoderException(DecoderException.DecoderExceptionType.EMPTY);
      }

      DecodedMessage.DecodedMessageBuilder builder =
          DecodedMessage.builder().decodedValue(data).encoding(DataEncoding.JSON);

      final JsonNode jsonValue = objectMapper.readTree(decodedString);

      switch (jsonValue.getNodeType()) {
        case ARRAY:
        case OBJECT:
          // allowed
          break;
        default:
          // all the rest is unknown
          throw new DecoderException(DecoderException.DecoderExceptionType.NOT_SUPPORTED);
      }

     if (jsonValue.size() == 0) {
        // empty object or array
        throw new DecoderException(DecoderException.DecoderExceptionType.EMPTY);
      }

      return Optional.of(builder.build());

    } catch (IOException ex) {
      LOG.trace("Unable to decode JSON data", ex);
      return Optional.empty();
    }
  }
}
