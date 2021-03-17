package io.spoud.agoora.agents.api.mapper;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.time.Instant;

@UtilityClass
public class StandardProtoMapper {

  @SneakyThrows
  public static StringValue stringValue(String value) {
    return StringValue.of(value);
  }

  @SneakyThrows
  public static Timestamp timestamp(Instant instant) {
    return Timestamp.newBuilder()
        .setSeconds(instant.getEpochSecond())
        .setNanos(instant.getNano())
        .build();
  }
}
