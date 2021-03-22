package io.spoud.agoora.agents.pgsql.utils;

import io.spoud.agoora.agents.pgsql.data.FieldDescription;
import io.spoud.agoora.agents.pgsql.data.JsonPropertyDescription;
import io.spoud.agoora.agents.pgsql.data.JsonSchema;
import io.spoud.agoora.agents.pgsql.data.TableDescription;
import lombok.experimental.UtilityClass;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@UtilityClass
public class SchemaUtil {

  /**
   * Convert from table description to json schema and try to keep the order
   *
   * @param tableDescription
   * @return
   */
  public static JsonSchema convertToJsonSchema(TableDescription tableDescription) {
    List<String> requiredFields =
        tableDescription.getFields().stream()
            .filter(not(FieldDescription::isNullable))
            .map(FieldDescription::getName)
            .collect(Collectors.toList());
    LinkedHashMap<String, JsonPropertyDescription> properties = new LinkedHashMap<>();
    tableDescription
        .getFields()
        .forEach(
            field ->
                properties.put(
                    field.getName(),
                    JsonPropertyDescription.builder().type(field.getType()).build()));
    return JsonSchema.builder().properties(properties).required(requiredFields).build();
  }
}
