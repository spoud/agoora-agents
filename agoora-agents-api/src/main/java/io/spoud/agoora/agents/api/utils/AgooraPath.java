package io.spoud.agoora.agents.api.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AgooraPath {
  public static final char SEPARATOR = '/';

  private final String resourceGroupPath;
  private final String name;

  public static AgooraPath parse(String absolutePath) {
    final int lastIndex = absolutePath.lastIndexOf(SEPARATOR);
    if (lastIndex == -1 || lastIndex == 0 || lastIndex == absolutePath.length() - 1) {
      throw new IllegalArgumentException("Invalid path '" + absolutePath + "' ");
    }
    return AgooraPath.builder()
        .resourceGroupPath(absolutePath.substring(0, lastIndex + 1))
        .name(absolutePath.substring(lastIndex + 1))
        .build();
  }

  public String getAbsolutePath() {
    return resourceGroupPath + name;
  }

  @Override
  public String toString() {
    return getAbsolutePath();
  }
}
