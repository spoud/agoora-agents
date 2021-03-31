package io.spoud.agoora.agents.kafka.decoder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DecodedMessage {
  private DataEncoding encoding;
  private byte[] decodedValue;
  @Builder.Default private boolean rootArray = false;
}
