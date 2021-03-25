package io.spoud.agoora.agents.openapi.swagger.data;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Builder
@Data
public class SwaggerSchema {
    private final List<Parameter> parameters;
    private final RequestBody requestBody;
    private final ApiResponses responses;
    private final Map<String, Schema> definitions;
}
