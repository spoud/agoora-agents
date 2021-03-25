package io.spoud.agoora.agents.openapi.jsonschema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spoud.agoora.agents.openapi.swagger.data.SwaggerSchema;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class SchemaUtil {

  public static final JsonPropertyDescription EMPTY_JSON_PROPERTY = new JsonPropertyDescription();
  private static final ObjectMapper OBJECT_MAPPER;

  static {
    OBJECT_MAPPER = new ObjectMapper();
  }

  public static String convertToJsonSchemaString(final SwaggerSchema schema)
      throws JsonProcessingException {
    final JsonSchema jsonSchema = SchemaUtil.convertToJsonSchema(schema);
    return OBJECT_MAPPER.writeValueAsString(jsonSchema);
  }

  public static JsonSchema convertToJsonSchema(final SwaggerSchema schema) {
    LinkedHashMap<String, JsonPropertyDescription> properties = new LinkedHashMap<>();

    getParameters(schema.getParameters()).ifPresent(p -> properties.put("parameters", p));
    getProduces(schema.getRequestBody()).ifPresent(p -> properties.put("produces", p));
    getBody(schema.getRequestBody(), schema.getDefinitions())
        .ifPresent(p -> properties.put("requestBody", p));
    getResponses(schema.getResponses(), schema.getDefinitions())
        .ifPresent(p -> properties.put("responses", p));

    return JsonSchema.builder().properties(properties).build();
  }

  public static Optional<JsonPropertyDescription> getParameters(final List<Parameter> parameter) {
    if (parameter != null) {
      parameter.sort(Comparator.comparing(Parameter::getName));
      LinkedHashMap<String, JsonPropertyDescription> properties = new LinkedHashMap<>();
      parameter.forEach(
          p ->
              properties.put(
                  p.getName(),
                  JsonPropertyDescription.builder().type(p.getSchema().getType()).build()));
      return Optional.of(JsonPropertyDescription.builder().properties(properties).build());
    }
    return Optional.empty();
  }

  public static Optional<JsonPropertyDescription> getProduces(final RequestBody requestBody) {
    if (requestBody != null && requestBody.getContent() != null) {
      final Set<String> produces = requestBody.getContent().keySet();
      LinkedHashMap<String, JsonPropertyDescription> properties = new LinkedHashMap<>();
      produces.forEach(p -> properties.put(p, JsonPropertyDescription.builder().build()));
      return Optional.of(JsonPropertyDescription.builder().properties(properties).build());
    }
    return Optional.empty();
  }

  public static Optional<JsonPropertyDescription> getBody(
      final RequestBody requestBody, final Map<String, Schema> definitions) {
    if (requestBody != null
        && requestBody.getContent() != null
        && !requestBody.getContent().isEmpty()) {
      final MediaType mediaType = requestBody.getContent().values().iterator().next();
      return getFromDefinition(mediaType.getSchema(), definitions);
    }
    return Optional.empty();
  }

  public static Optional<JsonPropertyDescription> getResponses(
      final ApiResponses responses, final Map<String, Schema> definitions) {
    if (responses != null && !responses.isEmpty()) {

      LinkedHashMap<String, JsonPropertyDescription> properties = new LinkedHashMap<>();
      responses.keySet().stream()
          .sorted()
          .forEach(
              key -> {
                final ApiResponse apiResponse = responses.get(key);
                if (apiResponse.getContent() != null
                    && !apiResponse.getContent().values().isEmpty()) {
                  final MediaType mediaType = apiResponse.getContent().values().iterator().next();
                  properties.put(
                      key,
                      getFromDefinition(mediaType.getSchema(), definitions)
                          .orElse(EMPTY_JSON_PROPERTY));
                } else if (apiResponse.get$ref() != null) {
                  properties.put(
                      key,
                      getDefinition(apiResponse.get$ref(), definitions)
                          .orElse(EMPTY_JSON_PROPERTY));
                } else {
                  properties.put(key, EMPTY_JSON_PROPERTY);
                }
              });
      return Optional.of(JsonPropertyDescription.builder().properties(properties).build());
    }
    return Optional.empty();
  }

  public static Optional<JsonPropertyDescription> getFromDefinition(
      final Schema schema, final Map<String, Schema> definitions) {

    if (schema.get$ref() != null) {
      // schema by ref => lookup
      return getDefinition(schema.get$ref(), definitions);
    }

    if (schema.getType().equals("object")) {
      // schema is an object, get all properties
      final Map<String, Schema> properties = schema.getProperties();
      if (properties == null) {
        return Optional.empty();
      }

      final Map<String, JsonPropertyDescription> props =
          properties.entrySet().stream()
              .filter(entry -> entry.getKey() != null)
              .map(
                  entry ->
                      Map.entry(entry.getKey(), getFromDefinition(entry.getValue(), definitions)))
              .filter(entry -> entry.getValue().isPresent())
              .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().get()));
      return Optional.of(
          JsonPropertyDescription.builder().type("object").properties(props).build());
    }

    if (schema.getType().equals("array")) {
      // schema is an array
      final ArraySchema arraySchema = (ArraySchema) schema;

      final JsonItems.JsonItemsBuilder builder =
          JsonItems.builder().type(arraySchema.getItems().getType());
      getFromDefinition(arraySchema.getItems(), definitions)
          .ifPresent(def -> builder.type(def.getType()).properties(def.getProperties()));

      return Optional.of(
          JsonPropertyDescription.builder().type(schema.getType()).items(builder.build()).build());
    }

    if (schema.getType() != null) {
      // type is present, use it
      return Optional.of(JsonPropertyDescription.builder().type(schema.getType()).build());
    }

    return Optional.empty();
  }

  public static Optional<JsonPropertyDescription> getDefinition(
      final String rawRef, final Map<String, Schema> definitions) {
    if (rawRef != null) {
      final int index = rawRef.lastIndexOf("/");
      final String ref = index == -1 ? rawRef : rawRef.substring(index+1);
      final Schema definition = definitions.get(ref);
      if (definition != null) {
        return getFromDefinition(definition, definitions);
      }else{
        LOG.error("Unable to find definition for ref ${}", rawRef);
      }
    } else {
      LOG.error("No ref defined");
    }
    return Optional.empty();
  }
}
