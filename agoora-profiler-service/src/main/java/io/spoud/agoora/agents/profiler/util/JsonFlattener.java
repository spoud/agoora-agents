package io.spoud.agoora.agents.profiler.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class JsonFlattener {

    private JsonFlattener() {}

    @SuppressWarnings("unchecked")
    public static Map<String, Object> flatten(Object value, String prefix) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value instanceof Map) {
            for (Map.Entry<String, Object> e : ((Map<String, Object>) value).entrySet()) {
                String key = prefix.isEmpty() ? e.getKey() : prefix + "/" + e.getKey();
                result.putAll(flatten(e.getValue(), key));
            }
        } else if (!prefix.isEmpty()) {
            result.put(prefix, value);
        }
        return result;
    }
}
