package io.spoud.agoora.agents.kafka.decoder;

import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Orders:
 *
 * <p>
 *
 * <ul>
 *   <li>json (10)
 *   <li>xml (12)
 *   <li>avro (20)
 *   <li>protobuf (30)
 * </ul>
 */
@Slf4j
@ApplicationScoped
public class DecoderService {

  public static final DecodedMessages NULL_DECODED_MESSAGE =
      DecodedMessages.builder()
          .messages(Collections.emptyList())
          .encoding(DataEncoding.UNKNOWN)
          .build();

  @Getter(AccessLevel.PROTECTED)
  private final List<SampleDecoder> sampleDecoders;

  public DecoderService(Instance<SampleDecoder> sampleDecoders) {
    this.sampleDecoders = sampleDecoders.stream().sorted().collect(Collectors.toList());
  }

  public DecodedMessages decodeKey(String topic, List<byte[]> data) throws DecoderException {
    return decode(topic, KafkaStreamPart.KEY, data);
  }

  public DecodedMessages decodeValue(String topic, List<byte[]> data) throws DecoderException {
    return decode(topic, KafkaStreamPart.VALUE, data);
  }

  public DecodedMessages decode(String topic, KafkaStreamPart part, List<byte[]> data)
      throws DecoderException {
    if (data == null || data.isEmpty()) {
      return NULL_DECODED_MESSAGE;
    }
    return sampleDecoders.stream()
        .map(
            decoder -> {
              Optional<DecodedMessages> ret = Optional.empty();
              try {
                ret = decoder.decode(topic, part, data);
              } catch (DecoderException ex) {
                throw ex;
              } catch (Exception ex) {
                // fallback if a decoder leak an exception
                LOG.error(
                    "Unable to use decoder '{}' for topic '{}' and part '{}': ",
                    decoder.getClass(),
                    topic,
                    part,
                    ex);
              }
              return ret;
            })
        .filter(optional -> optional.isPresent())
        .map(Optional::get)
        .findFirst()
        .orElseThrow(
            () ->
                new DecoderException(
                    DecoderException.DecoderExceptionType.NOT_SUPPORTED,
                    "No decoder found for the value of topic '" + topic + "'"));
  }
}
