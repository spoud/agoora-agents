package io.spoud.agoora.agents.profiler.service;

import io.spoud.agoora.agents.profiler.model.ColumnStats;
import io.spoud.agoora.agents.profiler.model.ColumnType;
import io.spoud.agoora.agents.profiler.model.ProfilingResult;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DataProfilerServiceTest {

    private final DataProfilerService profilerService = new DataProfilerService();

    @Test
    void computeDuplicateCount_detectsDuplicatesRegardlessOfFieldOrder() {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("a", 1);
        row1.put("b", 2);

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("b", 2);
        row2.put("a", 1);

        ProfilingResult result = profilerService.profile(List.of(row1, row2));

        assertThat(result.getDuplicateCount()).isEqualTo(1);
    }

    @Test
    void computeDuplicateCount_sameOrder_stillDetected() {
        Map<String, Object> row = Map.of("a", 1, "b", 2);

        ProfilingResult result = profilerService.profile(List.of(row, row, row));

        assertThat(result.getDuplicateCount()).isEqualTo(2);
    }

    @Test
    void computeStats_mixedIntAndDouble_classifiedAsNumber() {
        List<Map<String, Object>> records = List.of(
                Map.of("v", 1), Map.of("v", 2), Map.of("v", 3.5));

        ColumnStats col = findColumn(profilerService.profile(records), "v");

        assertThat(col.getType()).isEqualTo(ColumnType.NUMBER);
        assertThat(col.isNumeric()).isTrue();
        assertThat(col.getMin()).isNotNull();
        assertThat(col.getMax()).isNotNull();
        assertThat(col.getHistogram()).isNotNull();
        assertThat(col.getPercentiles()).isNotNull();
    }

    @Test
    void computeStats_pureIntegers_stillInteger() {
        List<Map<String, Object>> records = List.of(Map.of("v", 1), Map.of("v", 2));

        ColumnStats col = findColumn(profilerService.profile(records), "v");

        assertThat(col.getType()).isEqualTo(ColumnType.INTEGER);
        assertThat(col.getWarnings()).doesNotContain("NUMERIC_AS_STRING");
    }

    @Test
    void computeStats_genuinelyMixedStringAndNumber_stillMixed() {
        List<Map<String, Object>> records = List.of(Map.of("v", 1), Map.of("v", "x"));

        ColumnStats col = findColumn(profilerService.profile(records), "v");

        assertThat(col.getType()).isEqualTo(ColumnType.MIXED);
    }

    @Test
    void correlations_includeMixedNumericColumn() {
        List<Map<String, Object>> records = List.of(
                Map.of("a", 1, "b", 10),
                Map.of("a", 2, "b", 20),
                Map.of("a", 3.5, "b", 35),
                Map.of("a", 4, "b", 40));

        ProfilingResult result = profilerService.profile(records);

        assertThat(result.getCorrelations()).isNotNull();
        assertThat(result.getCorrelations().get("pearson").get("a")).containsKey("b");
    }

    @Test
    void computeStats_minMaxAvgCharLength() {
        List<Map<String, Object>> records = List.of(
                Map.of("v", "ab"),
                Map.of("v", "abcde"),
                Map.of("v", "abcdefghij"));

        ColumnStats col = findColumn(profilerService.profile(records), "v");

        assertThat(col.getMinCharLength()).isEqualTo(2.0);
        assertThat(col.getMaxCharLength()).isEqualTo(10.0);
        assertThat(col.getAvgCharLength()).isCloseTo(5.67, org.assertj.core.data.Offset.offset(0.1));
    }

    @Test
    void computeStats_epochSeconds_detectedAsTimestamp() {
        List<Map<String, Object>> records = List.of(
                Map.of("ts", 1700000000L),
                Map.of("ts", 1700100000L),
                Map.of("ts", 1700200000L));

        ColumnStats col = findColumn(profilerService.profile(records), "ts");

        assertThat(col.getType()).isEqualTo(ColumnType.TIMESTAMP);
        assertThat(col.getTimestampFormat()).isEqualTo("EPOCH_SECONDS");
        assertThat(col.getInterpretedMinDate()).isNotNull();
        assertThat(col.getInterpretedMaxDate()).isNotNull();
        assertThat(col.isNumeric()).isTrue();
        assertThat(col.getMin()).isNotNull();
    }

    @Test
    void computeStats_epochMillis_detectedAsTimestamp() {
        List<Map<String, Object>> records = List.of(
                Map.of("ts", 1700000000000L),
                Map.of("ts", 1700100000000L),
                Map.of("ts", 1700200000000L));

        ColumnStats col = findColumn(profilerService.profile(records), "ts");

        assertThat(col.getType()).isEqualTo(ColumnType.TIMESTAMP);
        assertThat(col.getTimestampFormat()).isEqualTo("EPOCH_MILLIS");
    }

    @Test
    void computeStats_regularIntegers_notTimestamp() {
        List<Map<String, Object>> records = List.of(
                Map.of("v", 1), Map.of("v", 2), Map.of("v", 3));

        ColumnStats col = findColumn(profilerService.profile(records), "v");

        assertThat(col.getType()).isEqualTo(ColumnType.INTEGER);
        assertThat(col.getTimestampFormat()).isNull();
    }

    @Test
    void computeRecordSizeStats() {
        List<String> rawJsonStrings = List.of(
                "{\"a\":1}",       // 7 bytes
                "{\"a\":100}",     // 9 bytes
                "{\"a\":10000}");  // 11 bytes

        ProfilingResult.RecordSizeStats stats = profilerService.computeRecordSizeStats(rawJsonStrings);

        assertThat(stats).isNotNull();
        assertThat(stats.minBytes()).isEqualTo(7);
        assertThat(stats.maxBytes()).isEqualTo(11);
        assertThat(stats.totalBytes()).isEqualTo(27);
        assertThat(stats.avgBytes()).isEqualTo(9.0);
    }

    @Test
    void computeRecordSizeStats_nullInput_returnsNull() {
        assertThat(profilerService.computeRecordSizeStats(null)).isNull();
    }

    @Test
    void profile_withRawJsonStrings_includesRecordSizeStats() {
        List<Map<String, Object>> records = List.of(
                Map.of("a", 1), Map.of("a", 2));
        List<String> rawJson = List.of("{\"a\":1}", "{\"a\":2}");

        ProfilingResult result = profilerService.profile(records, rawJson);

        assertThat(result.getRecordSizeStats()).isNotNull();
        assertThat(result.getRecordSizeStats().minBytes()).isGreaterThan(0);
    }

    @Test
    void profile_withoutRawJsonStrings_noRecordSizeStats() {
        List<Map<String, Object>> records = List.of(Map.of("a", 1));

        ProfilingResult result = profilerService.profile(records);

        assertThat(result.getRecordSizeStats()).isNull();
    }

    @Test
    void computeStats_numericStrings_detectedAsInteger() {
        List<Map<String, Object>> records = List.of(
                Map.of("v", "1"), Map.of("v", "2"), Map.of("v", "3"));

        ColumnStats col = findColumn(profilerService.profile(records), "v");

        assertThat(col.getType()).isEqualTo(ColumnType.INTEGER);
        assertThat(col.isNumeric()).isTrue();
        assertThat(col.getMin()).isEqualTo(1.0);
        assertThat(col.getMax()).isEqualTo(3.0);
        assertThat(col.getMean()).isEqualTo(2.0);
        assertThat(col.getWarnings()).contains("NUMERIC_AS_STRING");
    }

    @Test
    void computeStats_decimalStrings_detectedAsNumber() {
        List<Map<String, Object>> records = List.of(
                Map.of("v", "1.5"), Map.of("v", "2.7"), Map.of("v", "3.1"));

        ProfilingResult result = profilerService.profile(records);
        ColumnStats col = findColumn(result, "v");

        assertThat(col.getType()).isEqualTo(ColumnType.NUMBER);
        assertThat(col.isNumeric()).isTrue();
        assertThat(col.getMin()).isEqualTo(1.5);
        assertThat(col.getMax()).isEqualTo(3.1);
        assertThat(col.getWarnings()).contains("NUMERIC_AS_STRING");
        assertThat(result.getWarnings()).anyMatch(w ->
                "NUMERIC_AS_STRING".equals(w.code()) && "v".equals(w.column()));
    }

    @Test
    void computeStats_booleanStrings_detectedAsBoolean() {
        List<Map<String, Object>> records = List.of(
                Map.of("v", "true"), Map.of("v", "false"), Map.of("v", "True"));

        ColumnStats col = findColumn(profilerService.profile(records), "v");

        assertThat(col.getType()).isEqualTo(ColumnType.BOOLEAN);
        assertThat(col.getWarnings()).contains("BOOLEAN_AS_STRING");
    }

    @Test
    void computeStats_nativeBooleans_noBooleanAsStringWarning() {
        List<Map<String, Object>> records = List.of(
                Map.of("v", true), Map.of("v", false));

        ColumnStats col = findColumn(profilerService.profile(records), "v");

        assertThat(col.getType()).isEqualTo(ColumnType.BOOLEAN);
        assertThat(col.getWarnings()).doesNotContain("BOOLEAN_AS_STRING");
    }

    @Test
    void computeStats_isoOffsetDateTime_detectedAsDate() {
        List<Map<String, Object>> records = List.of(
                Map.of("v", "2024-01-15T10:30:00+01:00"),
                Map.of("v", "2024-06-20T14:00:00+02:00"));

        ColumnStats col = findColumn(profilerService.profile(records), "v");

        assertThat(col.getType()).isEqualTo(ColumnType.DATE);
    }

    @Test
    void computeStats_isoInstant_detectedAsDate() {
        List<Map<String, Object>> records = List.of(
                Map.of("v", "2024-01-15T10:30:00Z"),
                Map.of("v", "2024-06-20T14:00:00Z"));

        ColumnStats col = findColumn(profilerService.profile(records), "v");

        assertThat(col.getType()).isEqualTo(ColumnType.DATE);
    }

    @Test
    void computeStats_uuids_detectedAsUuid() {
        List<Map<String, Object>> records = List.of(
                Map.of("v", "a5a57bd8-0df3-48be-925e-f74239fe8acf"),
                Map.of("v", "b6b68ce9-1e04-59cf-a36f-085340ff9bd0"));

        ColumnStats col = findColumn(profilerService.profile(records), "v");

        assertThat(col.getType()).isEqualTo(ColumnType.UUID);
    }

    @Test
    void computeStats_stringEpochTimestamp_detectedAsTimestamp() {
        List<Map<String, Object>> records = List.of(
                Map.of("ts", "1700000000"),
                Map.of("ts", "1700100000"),
                Map.of("ts", "1700200000"));

        ColumnStats col = findColumn(profilerService.profile(records), "ts");

        assertThat(col.getType()).isEqualTo(ColumnType.TIMESTAMP);
        assertThat(col.getTimestampFormat()).isEqualTo("EPOCH_SECONDS");
        assertThat(col.getWarnings()).contains("NUMERIC_AS_STRING");
    }

    @Test
    void computeStats_mixedNumericStringsAndStrings_classifiedAsMixed() {
        List<Map<String, Object>> records = List.of(
                Map.of("v", "42"), Map.of("v", "hello"));

        ColumnStats col = findColumn(profilerService.profile(records), "v");

        assertThat(col.getType()).isEqualTo(ColumnType.MIXED);
    }

    private ColumnStats findColumn(ProfilingResult result, String name) {
        return result.getColumns().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
