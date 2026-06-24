package io.spoud.agoora.agents.profiler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.spoud.agoora.agents.profiler.util.JsonFlattener;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class JsonSchemaInferenceService {

    private final ObjectMapper objectMapper;

    /**
     * Infers a JSON Schema from a list of parsed JSON records.
     * Nested objects are represented with "/" separator in property names,
     * matching the Python json_normalize behaviour.
     */
    public String infer(List<Map<String, Object>> records) throws Exception {
        List<Map<String, Object>> flat = new ArrayList<>(records.size());
        for (Map<String, Object> record : records) {
            flat.add(JsonFlattener.flatten(record, ""));
        }
        return inferFromFlat(flat);
    }

    /**
     * Infers a JSON Schema from pre-flattened records, avoiding redundant flattening.
     */
    public String inferFromFlat(List<Map<String, Object>> flatRecords) throws Exception {
        Map<String, Set<String>> fieldTypes = new LinkedHashMap<>();

        for (Map<String, Object> flat : flatRecords) {
            for (Map.Entry<String, Object> e : flat.entrySet()) {
                fieldTypes.computeIfAbsent(e.getKey(), k -> new LinkedHashSet<>())
                        .add(jsonTypeOf(e.getValue()));
            }
        }

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("$schema", "http://json-schema.org/draft-07/schema#");
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();
        for (Map.Entry<String, Set<String>> entry : fieldTypes.entrySet()) {
            ObjectNode propNode = objectMapper.createObjectNode();
            Set<String> types = entry.getValue();

            if (types.size() == 1) {
                propNode.put("type", types.iterator().next());
            } else {
                // multiple observed types → use anyOf
                ArrayNode anyOf = objectMapper.createArrayNode();
                for (String t : types) {
                    anyOf.add(objectMapper.createObjectNode().put("type", t));
                }
                propNode.set("anyOf", anyOf);
            }
            properties.set(entry.getKey(), propNode);
        }
        schema.set("properties", properties);

        return objectMapper.writeValueAsString(schema);
    }

    private String jsonTypeOf(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof Integer || value instanceof Long) return "integer";
        if (value instanceof Number) return "number";
        return "string";
    }
}
