package io.spoud.agoora.agents.kafka.decoder;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DecoderUtilTest {

  @Test
  void testIgnoreBlankChar() {
    assertThat(DecoderUtil.getFirstPrintableCharacter(new byte[] {(byte) 0x09, (byte) 0x61}))
        .isEqualTo('a');
    assertThat(DecoderUtil.getFirstPrintableCharacter(new byte[] {(byte) 0x0A, (byte) 0x61}))
        .isEqualTo('a');
    assertThat(DecoderUtil.getFirstPrintableCharacter(new byte[] {(byte) 0x0D, (byte) 0x61}))
        .isEqualTo('a');
    assertThat(DecoderUtil.getFirstPrintableCharacter(new byte[] {(byte) 0x20, (byte) 0x61}))
        .isEqualTo('a');
    assertThat(
            DecoderUtil.getFirstPrintableCharacter(
                new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, (byte) 0x61}))
        .isEqualTo('a');

    assertThat(
            DecoderUtil.getFirstPrintableCharacter(
                new byte[] {
                  (byte) 0x09, (byte) 0x0A, (byte) 0x0D, (byte) 0x20, (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, (byte) 0x61
                }))
        .isEqualTo('a');
  }
}
