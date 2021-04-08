package io.spoud.agoora.agents.kafka.decoder.json;

import io.spoud.agoora.agents.kafka.decoder.DataEncoding;
import io.spoud.agoora.agents.kafka.decoder.DecodedMessages;
import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SampleDecoderJsonTest {

  private SampleDecoderJson sampleDecoderJson;

  @BeforeEach
  public void setup() {
    sampleDecoderJson = new SampleDecoderJson();
  }

  @Test
  public void decodeNull() {
    assertThat(
            sampleDecoderJson.decode(
                "topic",
                KafkaStreamPart.VALUE,
                Arrays.asList("null".getBytes(StandardCharsets.UTF_8))))
        .isEmpty();
  }

  @Test
  public void decodeEmpty() {
    assertThat(
            sampleDecoderJson.decode(
                "topic", KafkaStreamPart.VALUE, Arrays.asList("".getBytes(StandardCharsets.UTF_8))))
        .isEmpty();
  }

  @Test
  public void decodeEmptyObject() {
    assertThat(
            sampleDecoderJson.decode(
                "topic",
                KafkaStreamPart.VALUE,
                Arrays.asList("{}".getBytes(StandardCharsets.UTF_8))))
        .isEmpty();
  }

  @Test
  public void decodeEmptyArray() {
    assertThat(
            sampleDecoderJson.decode(
                "topic",
                KafkaStreamPart.VALUE,
                Arrays.asList("[]".getBytes(StandardCharsets.UTF_8))))
        .isEmpty();
  }

  @Test
  public void decodeString() {
    assertThat(
            sampleDecoderJson.decode(
                "topic",
                KafkaStreamPart.VALUE,
                Arrays.asList("\"hello\"".getBytes(StandardCharsets.UTF_8))))
        .isEmpty();
  }

  @Test
  public void decodeNumber() {
    assertThat(
            sampleDecoderJson.decode(
                "topic",
                KafkaStreamPart.VALUE,
                Arrays.asList("1".getBytes(StandardCharsets.UTF_8))))
        .isEmpty();
  }

  @Test
  public void decodeArray() {
    String content = "[{\"field\":1},{\"field\":2}]";
    Optional<DecodedMessages> message =
        sampleDecoderJson.decode(
            "topic",
            KafkaStreamPart.VALUE,
            Arrays.asList(content.getBytes(StandardCharsets.UTF_8)));
    assertThat(message).isPresent();
    assertThat(message.get().getEncoding()).isEqualTo(DataEncoding.JSON);
    assertThat(message.get().getUtf8String().get(0)).isEqualTo(content);
  }

  @Test
  public void decodeObject() {
    String content = "{\"field\":1}";
    Optional<DecodedMessages> message =
        sampleDecoderJson.decode(
            "topic",
            KafkaStreamPart.VALUE,
            Arrays.asList(content.getBytes(StandardCharsets.UTF_8)));
    assertThat(message).isPresent();
    assertThat(message.get().getEncoding()).isEqualTo(DataEncoding.JSON);
    assertThat(message.get().getUtf8String().get(0)).isEqualTo(content);
  }

  @Test
  public void decodeNonJson() {
    Optional<DecodedMessages> message =
        sampleDecoderJson.decode(
            "topic",
            KafkaStreamPart.VALUE,
            Arrays.asList("{abcd}".getBytes(StandardCharsets.UTF_8)));
    assertThat(message).isEmpty();
  }
}
