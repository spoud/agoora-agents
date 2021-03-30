package io.spoud.agoora.agents.kafka.decoder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class DecoderException extends RuntimeException {

  private final DecoderExceptionType type;

  public DecoderException(DecoderExceptionType type) {
    super(type.getErrorString());
    this.type = type;
  }

  public DecoderException(DecoderExceptionType type, String message) {
    super(message);
    this.type = type;
  }

  public DecoderException(DecoderExceptionType type, String message, Throwable cause) {
    super(message, cause);
    this.type = type;
  }

  @Getter
  @RequiredArgsConstructor
  public enum DecoderExceptionType {
    NULL("NullValue"),
    EMPTY("EmptyValue"),
    NOT_SUPPORTED("NotSupportedValue");
    final String errorString;
  }
}
