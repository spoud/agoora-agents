package io.spoud.agoora.agents.pgsql.config.converter;

import io.spoud.agoora.agents.api.utils.SdmPath;
import org.eclipse.microprofile.config.spi.Converter;

// TODO move to a common place
public class SdmPathConverter implements Converter<SdmPath> {

  @Override
  public SdmPath convert(String value) {
    return SdmPath.parse(value);
  }
}
