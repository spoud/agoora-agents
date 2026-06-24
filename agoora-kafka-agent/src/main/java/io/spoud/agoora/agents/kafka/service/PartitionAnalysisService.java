package io.spoud.agoora.agents.kafka.service;

import io.spoud.agoora.agents.kafka.data.KafkaSampleResult;
import io.spoud.agoora.agents.kafka.data.PartitionAnalysis;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class PartitionAnalysisService {

    public PartitionAnalysis analyze(KafkaSampleResult sampleResult) {
        Map<Integer, KafkaSampleResult.PartitionRange> ranges = sampleResult.getPartitionRanges();
        if (ranges == null || ranges.isEmpty()) {
            return PartitionAnalysis.builder()
                    .partitionCount(0)
                    .partitions(Collections.emptyMap())
                    .balanceScore(1.0)
                    .warnings(Collections.emptyList())
                    .build();
        }

        Map<Integer, Integer> sampledCounts = sampleResult.getRecords().stream()
                .collect(Collectors.groupingBy(KafkaSampleResult.KafkaRecord::getPartition,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        Map<Integer, PartitionAnalysis.PartitionStats> partitions = new LinkedHashMap<>();
        List<Long> messageCounts = new ArrayList<>();

        for (Map.Entry<Integer, KafkaSampleResult.PartitionRange> entry : ranges.entrySet()) {
            int partId = entry.getKey();
            long messageCount = entry.getValue().getMessageCount();
            messageCounts.add(messageCount);
            partitions.put(partId, PartitionAnalysis.PartitionStats.builder()
                    .messageCount(messageCount)
                    .sampledCount(sampledCounts.getOrDefault(partId, 0))
                    .build());
        }

        double mean = messageCounts.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = messageCounts.stream()
                .mapToDouble(c -> Math.pow(c - mean, 2))
                .average().orElse(0);
        double stddev = Math.sqrt(variance);
        double cv = mean > 0 ? stddev / mean : 0;
        double balanceScore = Math.max(0.0, Math.min(1.0, 1.0 - cv));

        long maxCount = messageCounts.stream().mapToLong(Long::longValue).max().orElse(0);
        long minCount = messageCounts.stream().mapToLong(Long::longValue).min().orElse(0);
        Double skewRatio = minCount > 0 ? (double) maxCount / minCount : null;

        List<String> warnings = new ArrayList<>();
        if (balanceScore < 0.7) {
            List<Integer> hotPartitions = partitions.entrySet().stream()
                    .filter(e -> e.getValue().getMessageCount() > mean * 1.5)
                    .map(Map.Entry::getKey)
                    .toList();
            List<Integer> coldPartitions = partitions.entrySet().stream()
                    .filter(e -> e.getValue().getMessageCount() < mean * 0.5)
                    .map(Map.Entry::getKey)
                    .toList();

            if (!hotPartitions.isEmpty() || !coldPartitions.isEmpty()) {
                StringBuilder warning = new StringBuilder("PARTITION_SKEW: ");
                if (!hotPartitions.isEmpty()) {
                    warning.append("hot partitions ").append(hotPartitions);
                }
                if (!coldPartitions.isEmpty()) {
                    if (!hotPartitions.isEmpty()) warning.append(", ");
                    warning.append("cold partitions ").append(coldPartitions);
                }
                warnings.add(warning.toString());
            } else {
                warnings.add(String.format("PARTITION_SKEW: uneven distribution (balance score %.2f)", balanceScore));
            }
        }

        return PartitionAnalysis.builder()
                .partitionCount(ranges.size())
                .partitions(partitions)
                .balanceScore(Math.round(balanceScore * 10000.0) / 10000.0)
                .skewRatio(skewRatio != null ? Math.round(skewRatio * 100.0) / 100.0 : null)
                .stddevMessages(stddev > 0 ? Math.round(stddev * 100.0) / 100.0 : null)
                .warnings(warnings.isEmpty() ? null : warnings)
                .build();
    }
}
