package io.spoud.agoora.agents.openapi.swagger.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SwaggerOperation {
  private final String method;
  private final String path;
  private final String url;
  private final SwaggerSchema schema;
}
