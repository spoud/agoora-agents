package io.spoud.agoora.agents.profiler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ColumnStats {

    private final String name;
    private final ColumnType type;
    private final int count;
    private final int missingCount;
    private final int uniqueCount;

    // numeric stats
    private final Double min;
    private final Double max;
    private final Double mean;
    private final Double median;
    private final Double stddev;
    private final Double skewness;
    private final Double kurtosis;
    private final Double iqr;
    private final Double range;
    private final Map<String, Double> percentiles;
    private final Histogram histogram;
    private final List<TopValue> extremeValuesHigh;
    private final List<TopValue> extremeValuesLow;

    // text/categorical stats
    private final List<TopValue> topValues;
    private final Double avgCharLength;
    private final Double minCharLength;
    private final Double maxCharLength;
    private final Double avgWordCount;

    // additional numeric stats (matching ydata-profiling)
    private final Double sum;
    private final Double variance;
    private final Double mad;
    private final Double cv;
    private final Integer negativeCount;
    private final Integer zeroCount;
    private final String monotonicity;

    // date stats
    private final String minDate;
    private final String maxDate;
    private final Long rangeDays;

    // timestamp stats (epoch detection)
    private final String timestampFormat;
    private final String interpretedMinDate;
    private final String interpretedMaxDate;

    // per-column warnings
    private final List<String> warnings;

    public double getMissingPercent() {
        return count == 0 ? 0.0 : (missingCount * 100.0 / count);
    }

    @JsonIgnore
    public boolean isNumeric() {
        return type == ColumnType.NUMBER || type == ColumnType.INTEGER || type == ColumnType.TIMESTAMP;
    }

    @JsonIgnore
    public boolean isCategorical() {
        return type == ColumnType.STRING || type == ColumnType.TEXT
                || type == ColumnType.CATEGORICAL || type == ColumnType.BOOLEAN
                || type == ColumnType.URL || type == ColumnType.EMAIL
                || type == ColumnType.UUID;
    }

    public record TopValue(String value, long count) {}

    public record Histogram(double[] bins, long[] counts) {}
}
