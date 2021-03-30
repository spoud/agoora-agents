package io.spoud.agoora.agents.kafka.schema;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum KafkaStreamPart {
  KEY("key"),
  VALUE("value");

  private final String subjectPostfix;

  @Override
  public String toString() {
    return subjectPostfix;
  }
}
