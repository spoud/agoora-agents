package io.spoud.agoora.agents.kafka.decoder.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import io.spoud.agoora.agents.kafka.decoder.DataEncoding;
import io.spoud.agoora.agents.kafka.decoder.DecodedMessages;
import io.spoud.agoora.agents.kafka.decoder.DecoderUtil;
import io.spoud.agoora.agents.kafka.decoder.SampleDecoder;
import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class SampleDecoderJson implements SampleDecoder {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public int getPriority() {
    return 10;
  }

  @Override
  public Optional<DecodedMessages> decode(
      String topic, KafkaStreamPart part, List<byte[]> dataList) {

    if (!eligible(dataList)) {
      LOG.trace("topic '{}' and part '{}' not eligible", topic, part);
      return Optional.empty();
    }

    List<byte[]> messages = new ArrayList<>(dataList.size());

    dataList.forEach(
        data -> {
          try {
            String decodedString = new String(data, StandardCharsets.UTF_8);

            if (decodedString.equalsIgnoreCase("null")) {
              LOG.warn("Value is null");
            } else if (decodedString.isBlank()) {
              LOG.warn("Value is empty");
            } else {

              final JsonNode jsonValue = objectMapper.readTree(decodedString);

              if (jsonValue.getNodeType() == JsonNodeType.ARRAY
                  || jsonValue.getNodeType() == JsonNodeType.OBJECT) {

                if (jsonValue.size() == 0) {
                  // empty object or array
                  LOG.warn("JSON is empty");
                } else {
                  messages.add(data);
                }
              } else {
                // all the rest is unknown
                LOG.warn("Unable to decode json value of type {}", jsonValue.getNodeType());
              }
            }

          } catch (IOException ex) {
            LOG.trace("Unable to decode JSON data", ex);
          }
        });
    if (messages.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(
          DecodedMessages.builder().encoding(DataEncoding.JSON).messages(messages).build());
    }
  }

  public boolean eligible(List<byte[]> list) {
    return DecoderUtil.checkEachFirstPrintableCharacter(list, c -> c == '[' || c == '{');
  }
}
