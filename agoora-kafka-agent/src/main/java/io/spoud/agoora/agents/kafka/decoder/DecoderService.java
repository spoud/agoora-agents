package io.spoud.agoora.agents.kafka.decoder;

import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class DecoderService {

  public static final DecodedMessage NULL_DECODED_MESSAGE =
      DecodedMessage.builder().decodedValue(null).encoding(DataEncoding.UNKNOWN).build();
  private final List<SampleDecoder> sampleDecoders;

  public DecoderService(Instance<SampleDecoder> sampleDecoders) {
    this.sampleDecoders = sampleDecoders.stream().sorted().collect(Collectors.toList());
  }

  public DecodedMessage decodeKey(String topic, byte[] data) throws DecoderException {
    return decode(topic, KafkaStreamPart.KEY, data);
  }

  public DecodedMessage decodeValue(String topic, byte[] data) throws DecoderException {
    return decode(topic, KafkaStreamPart.VALUE, data);
  }

  public DecodedMessage decode(String topic, KafkaStreamPart part, byte[] data)
      throws DecoderException {
    if (data == null) {
      return NULL_DECODED_MESSAGE;
    }
    return sampleDecoders.stream()
        .map(
            decoder -> {
              Optional<DecodedMessage> ret = Optional.empty();
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
