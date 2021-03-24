package io.spoud.agoora.agents.api.utils;

import org.eclipse.microprofile.config.spi.Converter;

public class SdmPathConverter implements Converter<SdmPath> {

  @Override
  public SdmPath convert(String value) {
    return SdmPath.parse(value);
  }
}
