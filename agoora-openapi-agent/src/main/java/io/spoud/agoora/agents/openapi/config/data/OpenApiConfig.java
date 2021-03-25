package io.spoud.agoora.agents.openapi.config.data;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OpenApiConfig {
  // Json URL
  private String url;

  // UI url for the deep dive tool
  private String uiUrl;
}
