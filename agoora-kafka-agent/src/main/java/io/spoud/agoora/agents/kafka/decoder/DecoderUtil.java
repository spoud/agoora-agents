package io.spoud.agoora.agents.kafka.decoder;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;

@UtilityClass
public class DecoderUtil {

  public static boolean checkEachFirstPrintableCharacter(
      List<byte[]> data, Predicate<Character> predicate) {
    return data.stream().map(DecoderUtil::getFirstPrintableCharacter).allMatch(predicate);
  }

  public static char getFirstPrintableCharacter(byte[] bytes) {
    final String utf8 = new String(bytes, StandardCharsets.UTF_8);
    for (int i = 0; i < utf8.length(); i++) {
      final char c = utf8.charAt(i);
      if (c == ' ' || c == '\n' || c == '\r' || c == '\u0009' || c == '\uFEFF') {
        continue;
      }
      return c;
    }
    return (char) 0;
  }
}
