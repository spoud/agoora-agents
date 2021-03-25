package io.spoud.agoora.agents.openapi.jsonschema;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@RegisterForReflection
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonPropertyDescription {

  private String type = "object";

  private Map<String, JsonPropertyDescription> properties;

  private JsonItems items;
}
