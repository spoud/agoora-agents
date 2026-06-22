package io.spoud.agoora.agents.profiler.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfilingResult {

    public record Warning(String code, String column, String message) {}
    public record RecordSizeStats(long minBytes, long maxBytes, double avgBytes, long totalBytes) {}

    private final List<ColumnStats> columns;
    private final int totalRecords;
    private final int duplicateCount;
    private final List<Map<String, Object>> sampleRowsHead;
    private final List<Map<String, Object>> sampleRowsTail;
    private final List<Warning> warnings;
    private final RecordSizeStats recordSizeStats;

    public ProfilingResult(
            List<ColumnStats> columns,
            int totalRecords,
            int duplicateCount,
            List<Map<String, Object>> sampleRowsHead,
            List<Map<String, Object>> sampleRowsTail,
            List<Warning> warnings,
            RecordSizeStats recordSizeStats) {
        this.columns = columns;
        this.totalRecords = totalRecords;
        this.duplicateCount = duplicateCount;
        this.sampleRowsHead = sampleRowsHead;
        this.sampleRowsTail = sampleRowsTail;
        this.warnings = warnings != null && !warnings.isEmpty() ? warnings : null;
        this.recordSizeStats = recordSizeStats;
    }

    public String getVersion() { return "3"; }
    public List<ColumnStats> getColumns() { return columns; }
    public int getTotalRecords() { return totalRecords; }
    public int getDuplicateCount() { return duplicateCount; }
    public List<Map<String, Object>> getSampleRowsHead() { return sampleRowsHead; }
    public List<Map<String, Object>> getSampleRowsTail() { return sampleRowsTail; }
    public List<Warning> getWarnings() { return warnings; }
    public RecordSizeStats getRecordSizeStats() { return recordSizeStats; }

    public long getTotalMissingValues() {
        return columns.stream().mapToLong(ColumnStats::getMissingCount).sum();
    }

    public double getOverallMissingPercent() {
        long total = (long) totalRecords * columns.size();
        return total == 0 ? 0.0 : (getTotalMissingValues() * 100.0 / total);
    }
}
