package io.spoud.agoora.agents.profiler.service;

import io.spoud.agoora.agents.profiler.model.ColumnStats;
import io.spoud.agoora.agents.profiler.model.ColumnType;
import io.spoud.agoora.agents.profiler.model.ProfilingResult;
import io.spoud.agoora.agents.profiler.util.JsonFlattener;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.correlation.KendallsCorrelation;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class DataProfilerService {

    private static final int HISTOGRAM_BINS = 10;
    private static final int TOP_VALUES_LIMIT = 10;
    private static final int EXTREME_VALUES_LIMIT = 5;
    private static final int SAMPLE_SIZE = 5;
    private static final int TYPE_SAMPLE_SIZE = 50;
    private static final double DATE_THRESHOLD = 0.8;
    private static final double URL_THRESHOLD = 0.8;
    private static final double EMAIL_THRESHOLD = 0.8;
    private static final double TEXT_CARDINALITY_RATIO = 0.5;
    private static final int CATEGORICAL_MAX_UNIQUE = 50;
    private static final int MAX_CORRELATION_COLUMNS = 100;

    private static final long EPOCH_SEC_MIN = 946684800L;        // 2000-01-01
    private static final long EPOCH_SEC_MAX = 2524608000L;        // 2050-01-01
    private static final long EPOCH_MILLIS_MIN = 946684800000L;
    private static final long EPOCH_MILLIS_MAX = 2524608000000L;
    private static final double TIMESTAMP_THRESHOLD = 0.8;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern URL_PATTERN =
            Pattern.compile("^https?://.*");
    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final double UUID_THRESHOLD = 0.8;

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_INSTANT,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")
    );

    public ProfilingResult profile(List<Map<String, Object>> records) {
        return profile(records, null, null);
    }

    public ProfilingResult profile(
            List<Map<String, Object>> flatRecords,
            List<Map<String, Object>> sampleRowsHead,
            List<Map<String, Object>> sampleRowsTail,
            ProfilingResult.RecordSizeStats recordSizeStats) {
        if (flatRecords.isEmpty()) {
            return new ProfilingResult(Collections.emptyList(), 0, 0,
                    Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyMap(), null);
        }

        Set<String> allColumns = new LinkedHashSet<>();
        flatRecords.forEach(r -> allColumns.addAll(r.keySet()));

        List<ColumnStats> columnStats = new ArrayList<>();
        List<String> numericCols = new ArrayList<>();
        List<String> categoricalCols = new ArrayList<>();

        for (String col : allColumns) {
            ColumnStats stats = computeStats(col, flatRecords);
            columnStats.add(stats);
            if (stats.isNumeric()) {
                numericCols.add(col);
            } else if (stats.isCategorical()) {
                categoricalCols.add(col);
            }
        }

        int duplicateCount = computeDuplicateCount(flatRecords);

        Map<String, Map<String, Map<String, Double>>> correlations;
        if (numericCols.size() + categoricalCols.size() > MAX_CORRELATION_COLUMNS) {
            LOG.info("Skipping correlations: {} columns exceeds limit of {}",
                    numericCols.size() + categoricalCols.size(), MAX_CORRELATION_COLUMNS);
            correlations = Collections.emptyMap();
        } else {
            Map<String, double[]> numericArrays = new LinkedHashMap<>();
            for (String col : numericCols) {
                numericArrays.put(col, extractNumericArray(col, flatRecords));
            }
            Map<String, List<String>> categoricalArrays = new LinkedHashMap<>();
            for (String col : categoricalCols) {
                categoricalArrays.put(col, extractCategoricalArray(col, flatRecords));
            }
            correlations = computeAllCorrelations(numericArrays, categoricalArrays);
        }

        List<ProfilingResult.Warning> warnings = computeDatasetWarnings(columnStats, duplicateCount, flatRecords.size());

        return new ProfilingResult(columnStats, flatRecords.size(), duplicateCount,
                sampleRowsHead, sampleRowsTail, warnings, correlations, recordSizeStats);
    }

    public ProfilingResult profile(List<Map<String, Object>> records, List<String> rawJsonStrings) {
        return profile(records, rawJsonStrings, null);
    }

    public ProfilingResult profile(List<Map<String, Object>> records, List<String> rawJsonStrings,
                                   ProfilingResult.RecordSizeStats precomputedRecordSizeStats) {
        if (records.isEmpty()) {
            return new ProfilingResult(Collections.emptyList(), 0, 0,
                    Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyMap(), null);
        }

        List<Map<String, Object>> flat = records.stream()
                .map(r -> JsonFlattener.flatten(r, ""))
                .collect(Collectors.toList());

        List<Map<String, Object>> head = records.subList(0, Math.min(SAMPLE_SIZE, records.size()));
        List<Map<String, Object>> tail = records.size() > SAMPLE_SIZE
                ? records.subList(Math.max(0, records.size() - SAMPLE_SIZE), records.size())
                : Collections.emptyList();

        ProfilingResult.RecordSizeStats recordSizeStats = precomputedRecordSizeStats != null
                ? precomputedRecordSizeStats
                : computeRecordSizeStats(rawJsonStrings);

        return profile(flat, head, tail, recordSizeStats);
    }

    // --- Type detection ---

    private ColumnType inferStringSubtype(String col, List<Map<String, Object>> flat) {
        List<String> nonNullSamples = flat.stream()
                .map(r -> r.get(col))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .limit(TYPE_SAMPLE_SIZE)
                .collect(Collectors.toList());

        if (nonNullSamples.isEmpty()) return ColumnType.STRING;

        // UUID detection
        long uuidMatches = nonNullSamples.stream()
                .filter(s -> UUID_PATTERN.matcher(s).matches()).count();
        if ((double) uuidMatches / nonNullSamples.size() >= UUID_THRESHOLD) return ColumnType.UUID;

        // Email detection
        long emailMatches = nonNullSamples.stream()
                .filter(s -> EMAIL_PATTERN.matcher(s).matches()).count();
        if ((double) emailMatches / nonNullSamples.size() >= EMAIL_THRESHOLD) return ColumnType.EMAIL;

        // URL detection
        long urlMatches = nonNullSamples.stream()
                .filter(s -> URL_PATTERN.matcher(s).matches()).count();
        if ((double) urlMatches / nonNullSamples.size() >= URL_THRESHOLD) return ColumnType.URL;

        // Date detection
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            long parsed = nonNullSamples.stream().filter(s -> tryParseDate(s, fmt)).count();
            if ((double) parsed / nonNullSamples.size() >= DATE_THRESHOLD) return ColumnType.DATE;
        }

        // Text vs Categorical — based on uniqueness ratio across entire column
        long nonNullCount = flat.stream().filter(r -> r.get(col) != null).count();
        long uniqueCount = flat.stream().map(r -> r.get(col)).filter(Objects::nonNull)
                .map(Object::toString).distinct().count();
        if (nonNullCount > 0 && (uniqueCount > CATEGORICAL_MAX_UNIQUE
                || (double) uniqueCount / nonNullCount > TEXT_CARDINALITY_RATIO)) {
            return ColumnType.TEXT;
        }
        return ColumnType.CATEGORICAL;
    }

    private static boolean isBooleanString(String s) {
        String lower = s.toLowerCase(Locale.ROOT);
        return "true".equals(lower) || "false".equals(lower);
    }

    private static Double tryParseNumber(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean tryParseDate(String s, DateTimeFormatter fmt) {
        try {
            fmt.parseBest(s, Instant::from, LocalDateTime::from, LocalDate::from);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // --- Per-column stats ---

    private ColumnStats computeStats(String col, List<Map<String, Object>> flat) {
        int count = flat.size();
        int missingCount = 0;
        boolean hasNumericStrings = false;
        boolean hasBooleanStrings = false;
        DescriptiveStatistics ds = new DescriptiveStatistics();
        Map<String, Long> freq = new LinkedHashMap<>();
        Set<String> observedTypes = new HashSet<>();

        for (Map<String, Object> row : flat) {
            Object val = row.get(col);
            if (val == null) {
                missingCount++;
                observedTypes.add("null");
                continue;
            }
            freq.merge(String.valueOf(val), 1L, (a, b) -> a + b);
            if (val instanceof Boolean) {
                observedTypes.add("boolean");
            } else if (val instanceof Integer || val instanceof Long) {
                observedTypes.add("integer");
                ds.addValue(((Number) val).doubleValue());
            } else if (val instanceof Number) {
                observedTypes.add("number");
                ds.addValue(((Number) val).doubleValue());
            } else {
                String str = val.toString().trim();
                Double parsed = tryParseNumber(str);
                if (parsed != null) {
                    hasNumericStrings = true;
                    boolean isInt = !str.contains(".") && !str.contains("e") && !str.contains("E");
                    observedTypes.add(isInt ? "integer" : "number");
                    ds.addValue(parsed);
                } else if (isBooleanString(str)) {
                    hasBooleanStrings = true;
                    observedTypes.add("boolean");
                } else {
                    observedTypes.add("string");
                }
            }
        }

        Set<String> nonNull = new HashSet<>(observedTypes);
        nonNull.remove("null");
        ColumnType type;
        if (nonNull.isEmpty()) {
            type = ColumnType.NULL;
        } else if (nonNull.equals(Set.of("integer"))) {
            type = ColumnType.INTEGER;
        } else if (Set.of("integer", "number").containsAll(nonNull)) {
            // pure "number", or whole numbers mixed with decimals — still one numeric column
            type = ColumnType.NUMBER;
        } else if (nonNull.size() > 1) {
            type = ColumnType.MIXED;
        } else {
            type = switch (nonNull.iterator().next()) {
                case "boolean" -> ColumnType.BOOLEAN;
                default -> inferStringSubtype(col, flat);
            };
        }

        // Timestamp detection for INTEGER/NUMBER columns
        String timestampFormat = null;
        if (type == ColumnType.INTEGER || type == ColumnType.NUMBER) {
            timestampFormat = detectTimestampFormat(ds);
            if (timestampFormat != null) {
                type = ColumnType.TIMESTAMP;
            }
        }

        ColumnStats.ColumnStatsBuilder builder = ColumnStats.builder()
                .name(col).type(type).count(count).missingCount(missingCount)
                .uniqueCount(freq.size());

        // Numeric stats
        if (ds.getN() > 0) {
            double[] vals = ds.getValues(); // in row order
            double min = ds.getMin(), max = ds.getMax();
            double p25 = ds.getPercentile(25), p75 = ds.getPercentile(75);
            double median = ds.getPercentile(50);
            double stddev = ds.getStandardDeviation();
            double mean = ds.getMean();
            int negCount = (int) Arrays.stream(vals).filter(v -> v < 0).count();
            int zeroCount = (int) Arrays.stream(vals).filter(v -> v == 0.0).count();
            builder.min(min).max(max)
                    .mean(mean)
                    .median(median)
                    .stddev(stddev)
                    .skewness(nanToNull(ds.getSkewness()))
                    .kurtosis(nanToNull(ds.getKurtosis()))
                    .iqr(p75 - p25)
                    .range(max - min)
                    .sum(ds.getSum())
                    .variance(nanToNull(ds.getVariance()))
                    .cv((!Double.isNaN(mean) && mean != 0) ? stddev / Math.abs(mean) : null)
                    .mad(computeMAD(vals, median))
                    .negativeCount(negCount > 0 ? negCount : null)
                    .zeroCount(zeroCount > 0 ? zeroCount : null)
                    .monotonicity(computeMonotonicity(vals))
                    .percentiles(Map.of(
                            "p05", ds.getPercentile(5),
                            "p25", p25,
                            "p75", p75,
                            "p95", ds.getPercentile(95)))
                    .histogram(buildHistogram(ds, min, max))
                    .extremeValuesHigh(buildExtremeValues(vals, true))
                    .extremeValuesLow(buildExtremeValues(vals, false));
        }

        // Timestamp interpretation
        if (type == ColumnType.TIMESTAMP && ds.getN() > 0 && timestampFormat != null) {
            builder.timestampFormat(timestampFormat);
            long minVal = (long) ds.getMin();
            long maxVal = (long) ds.getMax();
            if ("EPOCH_MILLIS".equals(timestampFormat)) {
                builder.interpretedMinDate(Instant.ofEpochMilli(minVal).atOffset(ZoneOffset.UTC).toLocalDate().toString());
                builder.interpretedMaxDate(Instant.ofEpochMilli(maxVal).atOffset(ZoneOffset.UTC).toLocalDate().toString());
            } else {
                builder.interpretedMinDate(Instant.ofEpochSecond(minVal).atOffset(ZoneOffset.UTC).toLocalDate().toString());
                builder.interpretedMaxDate(Instant.ofEpochSecond(maxVal).atOffset(ZoneOffset.UTC).toLocalDate().toString());
            }
        }

        // Top values (all types)
        List<ColumnStats.TopValue> topValues = freq.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_VALUES_LIMIT)
                .map(e -> new ColumnStats.TopValue(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        builder.topValues(topValues);

        // Text stats
        if (type == ColumnType.TEXT || type == ColumnType.STRING || type == ColumnType.CATEGORICAL) {
            DoubleSummaryStatistics charStats = flat.stream()
                    .map(r -> r.get(col)).filter(Objects::nonNull)
                    .mapToDouble(v -> v.toString().length()).summaryStatistics();
            if (charStats.getCount() > 0) {
                builder.avgCharLength(charStats.getAverage());
                builder.minCharLength(charStats.getMin());
                builder.maxCharLength(charStats.getMax());
                if (type == ColumnType.TEXT) {
                    DoubleSummaryStatistics wordStats = flat.stream()
                            .map(r -> r.get(col)).filter(Objects::nonNull)
                            .mapToDouble(v -> v.toString().trim().split("\\s+").length)
                            .summaryStatistics();
                    builder.avgWordCount(wordStats.getAverage());
                }
            }
        }

        // Date stats
        if (type == ColumnType.DATE) {
            List<LocalDate> dates = flat.stream()
                    .map(r -> r.get(col)).filter(Objects::nonNull)
                    .map(v -> parseToLocalDate(v.toString()))
                    .filter(Objects::nonNull)
                    .sorted().collect(Collectors.toList());
            if (!dates.isEmpty()) {
                LocalDate minD = dates.get(0), maxD = dates.get(dates.size() - 1);
                builder.minDate(minD.toString())
                        .maxDate(maxD.toString())
                        .rangeDays(ChronoUnit.DAYS.between(minD, maxD));
            }
        }

        // Per-column warnings
        builder.warnings(computeColumnWarnings(type, count, missingCount, freq, ds, hasNumericStrings, hasBooleanStrings));

        return builder.build();
    }

    // --- Array extraction for correlations ---

    private double[] extractNumericArray(String col, List<Map<String, Object>> flatRecords) {
        return flatRecords.stream()
                .mapToDouble(row -> {
                    Object val = row.get(col);
                    if (val instanceof Number) return ((Number) val).doubleValue();
                    if (val != null) {
                        Double d = tryParseNumber(val.toString().trim());
                        if (d != null) return d;
                    }
                    return Double.NaN;
                }).toArray();
    }

    private List<String> extractCategoricalArray(String col, List<Map<String, Object>> flatRecords) {
        return flatRecords.stream()
                .map(row -> {
                    Object val = row.get(col);
                    return val == null ? null : val.toString();
                })
                .collect(Collectors.toList());
    }

    // --- Helpers ---

    private List<ColumnStats.TopValue> buildExtremeValues(double[] values, boolean high) {
        double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        List<ColumnStats.TopValue> result = new ArrayList<>();
        if (high) {
            for (int i = sorted.length - 1; i >= 0 && result.size() < EXTREME_VALUES_LIMIT; i--) {
                result.add(new ColumnStats.TopValue(String.valueOf(sorted[i]), 1));
            }
        } else {
            for (int i = 0; i < sorted.length && result.size() < EXTREME_VALUES_LIMIT; i++) {
                result.add(new ColumnStats.TopValue(String.valueOf(sorted[i]), 1));
            }
        }
        return result;
    }

    private LocalDate parseToLocalDate(String s) {
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                var ta = fmt.parseBest(s, LocalDateTime::from, LocalDate::from);
                if (ta instanceof LocalDateTime ldt) return ldt.toLocalDate();
                if (ta instanceof LocalDate ld) return ld;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private ColumnStats.Histogram buildHistogram(DescriptiveStatistics ds, double min, double max) {
        if (min == max) {
            return new ColumnStats.Histogram(new double[]{min, max}, new long[]{ds.getN()});
        }
        double width = (max - min) / HISTOGRAM_BINS;
        double[] edges = new double[HISTOGRAM_BINS + 1];
        long[] counts = new long[HISTOGRAM_BINS];
        for (int i = 0; i <= HISTOGRAM_BINS; i++) edges[i] = min + i * width;
        for (double v : ds.getValues()) {
            int bin = (int) ((v - min) / width);
            if (bin >= HISTOGRAM_BINS) bin = HISTOGRAM_BINS - 1;
            counts[bin]++;
        }
        return new ColumnStats.Histogram(edges, counts);
    }

    private Double nanToNull(double v) { return Double.isNaN(v) ? null : v; }

    private Double computeMAD(double[] values, double median) {
        double[] deviations = Arrays.stream(values).map(v -> Math.abs(v - median)).toArray();
        DescriptiveStatistics ds = new DescriptiveStatistics(deviations);
        double mad = ds.getPercentile(50);
        return Double.isNaN(mad) ? null : mad;
    }

    private String computeMonotonicity(double[] values) {
        if (values.length < 2) return "CONSTANT";
        boolean increasing = true, decreasing = true;
        for (int i = 1; i < values.length; i++) {
            if (values[i] < values[i - 1]) increasing = false;
            if (values[i] > values[i - 1]) decreasing = false;
            if (!increasing && !decreasing) return "NOT_MONOTONIC";
        }
        if (increasing && decreasing) return "CONSTANT";
        return increasing ? "INCREASING" : "DECREASING";
    }

    // --- Duplicate detection ---

    private int computeDuplicateCount(List<Map<String, Object>> flat) {
        Set<Integer> seen = new HashSet<>();
        int dupes = 0;
        for (Map<String, Object> row : flat) {
            int hash = new TreeMap<>(row).hashCode();
            if (!seen.add(hash)) dupes++;
        }
        return dupes;
    }

    // --- Correlations ---

    private Map<String, Map<String, Map<String, Double>>> computeAllCorrelations(
            Map<String, double[]> numericArrays,
            Map<String, List<String>> categoricalArrays) {

        Map<String, Map<String, Map<String, Double>>> result = new LinkedHashMap<>();

        if (numericArrays.size() >= 2) {
            List<String> cols = new ArrayList<>(numericArrays.keySet());
            int n = numericArrays.get(cols.get(0)).length;
            List<Integer> validRows = validRowIndices(cols, numericArrays, n);

            if (validRows.size() >= 2) {
                double[][] matrix = buildMatrix(cols, numericArrays, validRows);
                addCorrelationMethod(result, "pearson", cols, computePearson(matrix));
                addCorrelationMethod(result, "spearman", cols, computeSpearman(matrix));
                addCorrelationMethod(result, "kendall", cols, computeKendall(cols, numericArrays, validRows));
            }
        }

        if (categoricalArrays.size() >= 2) {
            Map<String, Map<String, Double>> cramers = computeCramersV(categoricalArrays);
            if (!cramers.isEmpty()) result.put("cramersV", cramers);
        }

        return result;
    }

    private List<Integer> validRowIndices(List<String> cols, Map<String, double[]> arrays, int n) {
        List<Integer> valid = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            boolean ok = true;
            for (String col : cols) {
                if (Double.isNaN(arrays.get(col)[i])) { ok = false; break; }
            }
            if (ok) valid.add(i);
        }
        return valid;
    }

    private double[][] buildMatrix(List<String> cols, Map<String, double[]> arrays, List<Integer> validRows) {
        double[][] m = new double[validRows.size()][cols.size()];
        for (int r = 0; r < validRows.size(); r++) {
            int ri = validRows.get(r);
            for (int c = 0; c < cols.size(); c++) m[r][c] = arrays.get(cols.get(c))[ri];
        }
        return m;
    }

    private double[][] computePearson(double[][] matrix) {
        try { return new PearsonsCorrelation(matrix).getCorrelationMatrix().getData(); }
        catch (Exception e) { LOG.warn("Pearson failed: {}", e.getMessage()); return null; }
    }

    private double[][] computeSpearman(double[][] matrix) {
        try { return new SpearmansCorrelation().computeCorrelationMatrix(matrix).getData(); }
        catch (Exception e) { LOG.warn("Spearman failed: {}", e.getMessage()); return null; }
    }

    private double[][] computeKendall(List<String> cols, Map<String, double[]> arrays, List<Integer> validRows) {
        // KendallsCorrelation operates column-pair by column-pair
        try {
            int size = cols.size();
            double[][] m = new double[size][size];
            KendallsCorrelation kc = new KendallsCorrelation();
            for (int i = 0; i < size; i++) {
                m[i][i] = 1.0;
                for (int j = i + 1; j < size; j++) {
                    double[] a = extractValid(arrays.get(cols.get(i)), validRows);
                    double[] b = extractValid(arrays.get(cols.get(j)), validRows);
                    double corr = kc.correlation(a, b);
                    m[i][j] = corr;
                    m[j][i] = corr;
                }
            }
            return m;
        } catch (Exception e) { LOG.warn("Kendall failed: {}", e.getMessage()); return null; }
    }

    private double[] extractValid(double[] arr, List<Integer> validRows) {
        double[] out = new double[validRows.size()];
        for (int i = 0; i < validRows.size(); i++) out[i] = arr[validRows.get(i)];
        return out;
    }

    private void addCorrelationMethod(
            Map<String, Map<String, Map<String, Double>>> result,
            String method, List<String> cols, double[][] matrix) {
        if (matrix == null) return;
        Map<String, Map<String, Double>> methodMap = new LinkedHashMap<>();
        for (int i = 0; i < cols.size(); i++) {
            Map<String, Double> row = new LinkedHashMap<>();
            for (int j = 0; j < cols.size(); j++) {
                if (i != j && !Double.isNaN(matrix[i][j])) {
                    row.put(cols.get(j), Math.round(matrix[i][j] * 10000.0) / 10000.0);
                }
            }
            if (!row.isEmpty()) methodMap.put(cols.get(i), row);
        }
        if (!methodMap.isEmpty()) result.put(method, methodMap);
    }

    private Map<String, Map<String, Double>> computeCramersV(Map<String, List<String>> catArrays) {
        List<String> cols = new ArrayList<>(catArrays.keySet());
        Map<String, Map<String, Double>> result = new LinkedHashMap<>();
        for (int i = 0; i < cols.size(); i++) {
            for (int j = i + 1; j < cols.size(); j++) {
                String ca = cols.get(i), cb = cols.get(j);
                double v = cramersV(catArrays.get(ca), catArrays.get(cb));
                if (!Double.isNaN(v)) {
                    result.computeIfAbsent(ca, k -> new LinkedHashMap<>()).put(cb, round4(v));
                    result.computeIfAbsent(cb, k -> new LinkedHashMap<>()).put(ca, round4(v));
                }
            }
        }
        return result;
    }

    private double cramersV(List<String> colA, List<String> colB) {
        // Collect unique categories
        List<String> catsA = colA.stream().filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
        List<String> catsB = colB.stream().filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
        int k = catsA.size(), r = catsB.size();
        if (k < 2 || r < 2) return Double.NaN;

        Map<String, Integer> idxA = new HashMap<>();
        for (int i = 0; i < catsA.size(); i++) idxA.put(catsA.get(i), i);
        Map<String, Integer> idxB = new HashMap<>();
        for (int i = 0; i < catsB.size(); i++) idxB.put(catsB.get(i), i);

        long[][] table = new long[k][r];
        int n = 0;
        for (int row = 0; row < colA.size(); row++) {
            String a = colA.get(row), b = colB.get(row);
            if (a != null && b != null) { table[idxA.get(a)][idxB.get(b)]++; n++; }
        }
        if (n == 0) return Double.NaN;

        long[] rowSums = new long[k], colSums = new long[r];
        for (int ri = 0; ri < k; ri++)
            for (int ci = 0; ci < r; ci++) { rowSums[ri] += table[ri][ci]; colSums[ci] += table[ri][ci]; }

        double chi2 = 0;
        for (int ri = 0; ri < k; ri++)
            for (int ci = 0; ci < r; ci++) {
                double exp = (double) rowSums[ri] * colSums[ci] / n;
                if (exp > 0) { double d = table[ri][ci] - exp; chi2 += d * d / exp; }
            }

        int minDim = Math.min(k, r) - 1;
        return minDim == 0 ? Double.NaN : Math.sqrt(chi2 / ((double) n * minDim));
    }

    // --- Warnings ---

    private List<String> computeColumnWarnings(
            ColumnType type, int count, int missingCount,
            Map<String, Long> freq, DescriptiveStatistics ds,
            boolean hasNumericStrings, boolean hasBooleanStrings) {
        List<String> w = new ArrayList<>();
        if (count == 0) return w;

        if (hasNumericStrings) w.add("NUMERIC_AS_STRING");
        if (hasBooleanStrings) w.add("BOOLEAN_AS_STRING");

        double missingPct = missingCount * 100.0 / count;
        if (missingPct > 20) w.add("HIGH_MISSING");
        if (freq.size() == 1) w.add("CONSTANT");
        if (freq.size() == count - missingCount && freq.size() > 1) w.add("ALL_UNIQUE");

        if (type == ColumnType.STRING || type == ColumnType.CATEGORICAL || type == ColumnType.TEXT) {
            long nonMissing = count - missingCount;
            if (nonMissing > 0 && freq.size() > nonMissing * 0.9) w.add("HIGH_CARDINALITY");
            Optional<Map.Entry<String, Long>> dominant = freq.entrySet().stream()
                    .max(Map.Entry.comparingByValue());
            dominant.ifPresent(d -> {
                if (nonMissing > 0 && d.getValue() * 100.0 / nonMissing > 90) w.add("IMBALANCED");
            });
        }

        if (ds.getN() > 0) {
            Double skew = nanToNull(ds.getSkewness());
            if (skew != null && Math.abs(skew) > 1) w.add(skew > 0 ? "SKEWED_POSITIVE" : "SKEWED_NEGATIVE");
            long zeroCount = Arrays.stream(ds.getValues()).filter(v -> v == 0.0).count();
            if (zeroCount * 100.0 / ds.getN() > 50) w.add("HIGH_ZEROS");
        }

        return w.isEmpty() ? null : w;
    }

    private List<ProfilingResult.Warning> computeDatasetWarnings(
            List<ColumnStats> columns, int duplicateCount, int totalRecords) {
        List<ProfilingResult.Warning> w = new ArrayList<>();
        if (duplicateCount > 0) {
            w.add(new ProfilingResult.Warning("DUPLICATE_ROWS", null,
                    duplicateCount + " duplicate rows (" +
                    String.format("%.1f", duplicateCount * 100.0 / totalRecords) + "%)"));
        }
        for (ColumnStats col : columns) {
            if (col.getWarnings() != null) {
                for (String code : col.getWarnings()) {
                    w.add(new ProfilingResult.Warning(code, col.getName(), warningMessage(code, col)));
                }
            }
        }
        return w;
    }

    private String warningMessage(String code, ColumnStats col) {
        return switch (code) {
            case "HIGH_MISSING" -> String.format("%.1f%% missing values", col.getMissingPercent());
            case "CONSTANT" -> "Column has a single constant value";
            case "ALL_UNIQUE" -> "All values are unique (possible ID column)";
            case "HIGH_CARDINALITY" -> "Very high cardinality — possible free-text or ID";
            case "IMBALANCED" -> "Dominant category > 90% of values";
            case "SKEWED_POSITIVE" -> String.format("High positive skew (%.2f)", col.getSkewness());
            case "SKEWED_NEGATIVE" -> String.format("High negative skew (%.2f)", col.getSkewness());
            case "HIGH_ZEROS" -> "More than 50% zero values";
            case "NUMERIC_AS_STRING" -> "Values are numeric but encoded as strings in the schema";
            case "BOOLEAN_AS_STRING" -> "Values are boolean but encoded as strings in the schema";
            default -> code;
        };
    }

    private double round4(double v) { return Math.round(v * 10000.0) / 10000.0; }

    // --- Timestamp detection ---

    private String detectTimestampFormat(DescriptiveStatistics ds) {
        if (ds.getN() == 0) return null;

        double[] values = ds.getValues();
        int sampleSize = (int) Math.min(values.length, TYPE_SAMPLE_SIZE);

        long millisMatches = 0;
        long secondsMatches = 0;
        for (int i = 0; i < sampleSize; i++) {
            long v = (long) values[i];
            if (v >= EPOCH_MILLIS_MIN && v <= EPOCH_MILLIS_MAX) millisMatches++;
            if (v >= EPOCH_SEC_MIN && v <= EPOCH_SEC_MAX) secondsMatches++;
        }

        if ((double) millisMatches / sampleSize >= TIMESTAMP_THRESHOLD) return "EPOCH_MILLIS";
        if ((double) secondsMatches / sampleSize >= TIMESTAMP_THRESHOLD) return "EPOCH_SECONDS";
        return null;
    }

    // --- Record size stats ---

    ProfilingResult.RecordSizeStats computeRecordSizeStats(List<String> rawJsonStrings) {
        if (rawJsonStrings == null || rawJsonStrings.isEmpty()) return null;

        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        long total = 0;
        for (String json : rawJsonStrings) {
            long size = json.getBytes(StandardCharsets.UTF_8).length;
            if (size < min) min = size;
            if (size > max) max = size;
            total += size;
        }
        double avg = (double) total / rawJsonStrings.size();
        return new ProfilingResult.RecordSizeStats(min, max, avg, total);
    }
}
