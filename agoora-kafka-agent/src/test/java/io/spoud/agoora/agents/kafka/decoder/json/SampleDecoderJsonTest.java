package io.spoud.agoora.agents.kafka.decoder.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.spoud.agoora.agents.kafka.decoder.DataEncoding;
import io.spoud.agoora.agents.kafka.decoder.DecodedMessage;
import io.spoud.agoora.agents.kafka.decoder.DecoderException;
import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SampleDecoderJsonTest {

    private ObjectMapper objectMapper = new ObjectMapper();
    private SampleDecoderJson sampleDecoderJson;

    @BeforeEach
    public void setup() {
        sampleDecoderJson = new SampleDecoderJson(objectMapper);
    }

    @Test
    public void decodeNull() {
        assertThatThrownBy(
                () ->
                        sampleDecoderJson.decode(
                                "topic", KafkaStreamPart.VALUE, "null".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DecoderException.class)
                .extracting(e -> ((DecoderException) e).getType())
                .isEqualTo(DecoderException.DecoderExceptionType.NULL);
    }

    @Test
    public void decodeEmpty() {
        assertThatThrownBy(
                () ->
                        sampleDecoderJson.decode(
                                "topic", KafkaStreamPart.VALUE, "".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DecoderException.class)
                .extracting(e -> ((DecoderException) e).getType())
                .isEqualTo(DecoderException.DecoderExceptionType.EMPTY);
    }

    @Test
    public void decodeEmptyObject() {
        assertThatThrownBy(
                () ->
                        sampleDecoderJson.decode(
                                "topic", KafkaStreamPart.VALUE, "{}".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DecoderException.class)
                .extracting(e -> ((DecoderException) e).getType())
                .isEqualTo(DecoderException.DecoderExceptionType.EMPTY);
    }

    @Test
    public void decodeEmptyArray() {
        assertThatThrownBy(
                () ->
                        sampleDecoderJson.decode(
                                "topic", KafkaStreamPart.VALUE, "[]".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DecoderException.class)
                .extracting(e -> ((DecoderException) e).getType())
                .isEqualTo(DecoderException.DecoderExceptionType.EMPTY);
    }

    @Test
    public void decodeString() {
        assertThatThrownBy(
                () ->
                        sampleDecoderJson.decode(
                                "topic", KafkaStreamPart.VALUE, "\"hello\"".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DecoderException.class)
                .extracting(e -> ((DecoderException) e).getType())
                .isEqualTo(DecoderException.DecoderExceptionType.NOT_SUPPORTED);
    }

    @Test
    public void decodeNumber() {
        assertThatThrownBy(
                () ->
                        sampleDecoderJson.decode(
                                "topic", KafkaStreamPart.VALUE, "1".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DecoderException.class)
                .extracting(e -> ((DecoderException) e).getType())
                .isEqualTo(DecoderException.DecoderExceptionType.NOT_SUPPORTED);
    }

    @Test
    public void decodeArray() {
        String content = "[{\"field\":1},{\"field\":2}]";
        Optional<DecodedMessage> message =
                sampleDecoderJson.decode(
                        "topic", KafkaStreamPart.VALUE, content.getBytes(StandardCharsets.UTF_8));
        assertThat(message).isPresent();
        assertThat(message.get().getEncoding()).isEqualTo(DataEncoding.JSON);
        assertThat(message.get().getUtf8String()).isEqualTo(content);
    }

    @Test
    public void decodeObject() {
        String content = "{\"field\":1}";
        Optional<DecodedMessage> message =
                sampleDecoderJson.decode(
                        "topic", KafkaStreamPart.VALUE, content.getBytes(StandardCharsets.UTF_8));
        assertThat(message).isPresent();
        assertThat(message.get().getEncoding()).isEqualTo(DataEncoding.JSON);
        assertThat(message.get().getUtf8String()).isEqualTo(content);
    }

    @Test
    public void decodeNonJson() {
        Optional<DecodedMessage> message =
                sampleDecoderJson.decode(
                        "topic", KafkaStreamPart.VALUE, "{abcd}".getBytes(StandardCharsets.UTF_8));
        assertThat(message).isEmpty();
    }
}
