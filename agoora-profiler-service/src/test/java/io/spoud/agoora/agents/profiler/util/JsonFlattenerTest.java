package io.spoud.agoora.agents.profiler.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonFlattenerTest {

    @Test
    void flatten_nestedObject_joinsWithSlash() {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("name", "a");
        user.put("city", "b");
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("user", user);

        Map<String, Object> flat = JsonFlattener.flatten(record, "");

        assertThat(flat).containsExactly(
                Map.entry("user/name", "a"),
                Map.entry("user/city", "b"));
    }

    @Test
    void flatten_nullLeaf_preserved() {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("a", null);

        Map<String, Object> flat = JsonFlattener.flatten(record, "");

        assertThat(flat).containsKey("a");
        assertThat(flat.get("a")).isNull();
    }

    @Test
    void flatten_arrayLeaf_keptAsIs() {
        List<Integer> array = List.of(1, 2, 3);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("a", array);

        Map<String, Object> flat = JsonFlattener.flatten(record, "");

        assertThat(flat).containsEntry("a", array);
    }

    @Test
    void flatten_emptyMap_returnsEmptyMap() {
        assertThat(JsonFlattener.flatten(new LinkedHashMap<>(), "")).isEmpty();
    }
}
