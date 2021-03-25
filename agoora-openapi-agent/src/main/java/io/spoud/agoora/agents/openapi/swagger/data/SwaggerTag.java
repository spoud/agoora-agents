package io.spoud.agoora.agents.openapi.swagger.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SwaggerTag {
  private final String name;
  private final String description;
  private final String url;
}
