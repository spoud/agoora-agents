package io.spoud.agoora.agents.kafka.utils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@UtilityClass
public class ResourceUtil {

  @SneakyThrows
  public static String getFile(String file) {
    final InputStream resourceAsStream =
        ResourceUtil.class.getClassLoader().getResourceAsStream(file);
    if (resourceAsStream == null) {
      throw new IllegalArgumentException("File '" + file + "' not found");
    }
    return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
  }
}
