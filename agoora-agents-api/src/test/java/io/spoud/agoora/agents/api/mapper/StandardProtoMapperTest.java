package io.spoud.agoora.agents.api.mapper;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class StandardProtoMapperTest {

  @Test
  public void testString() {
    assertThat(StandardProtoMapper.stringValue("test").getValue()).isEqualTo("test");
  }

  @Test
  public void testTimesamp() {
    final Timestamp timestamp = StandardProtoMapper.timestamp(Instant.ofEpochMilli(12346578900L));
    assertThat(timestamp.getSeconds()).isEqualTo(12346578L);
    assertThat(timestamp.getNanos()).isEqualTo(900000000L);
  }
}
