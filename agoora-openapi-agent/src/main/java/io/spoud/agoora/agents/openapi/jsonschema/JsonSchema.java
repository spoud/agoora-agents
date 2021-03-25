package io.spoud.agoora.agents.openapi.jsonschema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;

@RegisterForReflection
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonSchema {

  @JsonProperty("$schema")
  private String schema = "http://json-schema.org/schema#";

  private String type = "object";

  // Use linked hashmap to keep order
  private LinkedHashMap<String, JsonPropertyDescription> properties;

  private List<String> required;
}
