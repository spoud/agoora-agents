package io.spoud.agoora.agents.openapi.swagger;

import io.spoud.agoora.agents.openapi.config.data.OpenApiConfig;
import io.spoud.agoora.agents.openapi.swagger.data.SwaggerOperation;
import io.spoud.agoora.agents.openapi.swagger.data.SwaggerSchema;
import io.spoud.agoora.agents.openapi.swagger.data.SwaggerTag;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SwaggerScrapper {

  private final OpenAPI openAPI;
  private final OpenApiConfig config;

  public SwaggerScrapper(OpenApiConfig config) {
    this.config = config;
    SwaggerParseResult result = new OpenAPIV3Parser().readLocation(config.url(), null, null);
    this.openAPI = result.getOpenAPI();
  }

  public Map<String, Schema> getDefinitions() {
    return openAPI.getComponents().getSchemas();
  }

  public List<SwaggerTag> getEndpoints() {
    return openAPI.getTags().stream()
        .map(
            tag -> SwaggerTag.builder()
                .name(tag.getName())
                .description(tag.getDescription())
                .url(config.url())
                .build())
        .collect(Collectors.toList());
  }

  public List<SwaggerOperation> getOperationsForEndpoint(SwaggerTag tag) {
    return openAPI.getPaths().entrySet().stream()
        .flatMap(
            entry ->
                entry.getValue().readOperationsMap().entrySet().stream()
                    .map(op -> Map.entry(entry.getKey(), op)))
        .filter(op -> op.getValue().getValue().getTags().contains(tag.getName()))
        .map(
            entry -> {
              final PathItem.HttpMethod method = entry.getValue().getKey();
              final String path = entry.getKey();
              final Operation operation = entry.getValue().getValue();
              return SwaggerOperation.builder()
                  .method(method.toString())
                  .path(path)
                  .url(path)
                  .schema(
                      SwaggerSchema.builder()
                          .parameters(operation.getParameters())
                          .requestBody(operation.getRequestBody())
                          .responses(operation.getResponses())
                          .definitions(getDefinitions())
                          .build())
                  .build();
            })
        .collect(Collectors.toList());
  }
}
