package io.spoud.agoora.agents.kafka.decoder.xml;

import com.ctc.wstx.stax.WstxInputFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.spoud.agoora.agents.kafka.decoder.DataEncoding;
import io.spoud.agoora.agents.kafka.decoder.DecodedMessages;
import io.spoud.agoora.agents.kafka.decoder.DecoderException;
import io.spoud.agoora.agents.kafka.decoder.DecoderUtil;
import io.spoud.agoora.agents.kafka.decoder.SampleDecoder;
import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class SampleDecoderXml implements SampleDecoder {

  public static final Charset CHARSET = StandardCharsets.UTF_8;
  private final XmlMapper xmlMapper = new XmlMapper(new WstxInputFactory());
  private final ObjectMapper jsonMapper = new ObjectMapper();

  @Override
  public int getPriority() {
    return 12;
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
            String decodedString = new String(data, CHARSET);

            if (decodedString.equalsIgnoreCase("null")) {
              LOG.warn("Value is null");
            } else if (decodedString.isBlank()) {
              LOG.warn("Value is empty");
            } else {
              final JsonNode xmlValue = xmlMapper.readTree(decodedString);

              if (xmlValue.isEmpty()) {
                // empty object or array
                LOG.warn("Xml value is empty");
              } else {
                try {
                  messages.add(jsonMapper.writeValueAsString(xmlValue).getBytes(CHARSET));
                } catch (Exception ex) {
                  throw new DecoderException(DecoderException.DecoderExceptionType.NOT_SUPPORTED);
                }
              }
            }
          } catch (IOException ex) {
            LOG.warn("Unable to decode XML data", ex);
          }
        });
    if (messages.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(
          DecodedMessages.builder().encoding(DataEncoding.XML).messages(messages).build());
    }
  }

  public boolean eligible(List<byte[]> list) {
    return DecoderUtil.checkEachFirstPrintableCharacter(list, c -> c == '<');
  }
}
