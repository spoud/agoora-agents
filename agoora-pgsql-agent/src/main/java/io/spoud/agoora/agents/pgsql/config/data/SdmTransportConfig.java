package io.spoud.agoora.agents.pgsql.config.data;

import io.spoud.agoora.agents.api.utils.SdmPath;
import lombok.Data;

@Data
public class SdmTransportConfig {

  private String sdmPath;

  public SdmPath getSdmPathObject(){
    return SdmPath.parse(sdmPath);
  }

}
