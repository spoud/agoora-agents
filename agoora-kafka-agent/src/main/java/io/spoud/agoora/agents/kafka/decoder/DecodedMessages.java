package io.spoud.agoora.agents.kafka.decoder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DecodedMessages {
  private DataEncoding encoding;
  private List<byte[]> messages;

  public List<String> getUtf8String() {
    return messages.stream()
        .map(decodedValue -> new String(decodedValue, StandardCharsets.UTF_8))
        .collect(Collectors.toList());
  }
}
