package io.spoud.agoora.agents.kafka.decoder.xml;

import io.spoud.agoora.agents.kafka.decoder.DataEncoding;
import io.spoud.agoora.agents.kafka.decoder.DecodedMessages;
import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SampleDecoderXmlTest {
  private SampleDecoderXml sampleDecoderXml;

  @BeforeEach
  public void setup() {
    sampleDecoderXml = new SampleDecoderXml();
  }

  @Test
  public void decodeNull() {
    assertThat(
            sampleDecoderXml.decode(
                "topic",
                KafkaStreamPart.VALUE,
                Arrays.asList("null".getBytes(StandardCharsets.UTF_8))))
        .isEmpty();
  }

  @Test
  public void decodeEmpty() {
    assertThat(
            sampleDecoderXml.decode(
                "topic", KafkaStreamPart.VALUE, Arrays.asList("".getBytes(StandardCharsets.UTF_8))))
        .isEmpty();
  }

  @Test
  public void decodeEmptyObject() {
    assertThat(
            sampleDecoderXml.decode(
                "topic",
                KafkaStreamPart.VALUE,
                Arrays.asList("<object />".getBytes(StandardCharsets.UTF_8))))
        .isEmpty();
  }

  @Test
  public void decodeNonXML() {
    assertThat(
            sampleDecoderXml.decode(
                "topic",
                KafkaStreamPart.VALUE,
                Arrays.asList("\"hello\"".getBytes(StandardCharsets.UTF_8))))
        .isEmpty();
    assertThat(
            sampleDecoderXml.decode(
                "topic",
                KafkaStreamPart.VALUE,
                Arrays.asList("1".getBytes(StandardCharsets.UTF_8))))
        .isEmpty();
  }

  @Test
  public void decodeObject() {
    String content = "<whatever><field>1</field></whatever>";
    Optional<DecodedMessages> message =
        sampleDecoderXml.decode(
            "topic",
            KafkaStreamPart.VALUE,
            Arrays.asList(content.getBytes(StandardCharsets.UTF_8)));
    assertThat(message).isPresent();
    assertThat(message.get().getEncoding()).isEqualTo(DataEncoding.XML);
    assertThat(message.get().getUtf8String().get(0)).isEqualTo("{\"field\":\"1\"}");
  }

  @Test
  public void decodeObjectAttribute() {
    String content = "<whatever field=\"1\"></whatever>";
    Optional<DecodedMessages> message =
        sampleDecoderXml.decode(
            "topic",
            KafkaStreamPart.VALUE,
            Arrays.asList(content.getBytes(StandardCharsets.UTF_8)));
    assertThat(message).isPresent();
    assertThat(message.get().getEncoding()).isEqualTo(DataEncoding.XML);
    assertThat(message.get().getUtf8String().get(0)).isEqualTo("{\"field\":\"1\"}");
  }

  @Test
  public void decodeNonXml() {
    Optional<DecodedMessages> message =
        sampleDecoderXml.decode(
            "topic",
            KafkaStreamPart.VALUE,
            Arrays.asList("{\"field\":\"abcd\"}".getBytes(StandardCharsets.UTF_8)));
    assertThat(message).isEmpty();
  }
}
