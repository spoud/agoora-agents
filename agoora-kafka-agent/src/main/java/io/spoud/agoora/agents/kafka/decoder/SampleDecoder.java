package io.spoud.agoora.agents.kafka.decoder;

import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;

import java.util.List;
import java.util.Optional;

/** Sample decoder interface */
public interface SampleDecoder extends Comparable<SampleDecoder> {

  int getPriority();

  Optional<DecodedMessages> decode(
      String topic, KafkaStreamPart kafkaStreamPart, List<byte[]> data) throws DecoderException;

  /**
   * Highest priority first
   *
   * @param other
   * @return
   */
  @Override
  default int compareTo(SampleDecoder other) {
    return -Integer.compare(getPriority(), other.getPriority());
  }
}
