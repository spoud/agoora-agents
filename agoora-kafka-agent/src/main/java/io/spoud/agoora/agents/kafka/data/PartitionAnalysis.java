package io.spoud.agoora.agents.kafka.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartitionAnalysis {
    private final int partitionCount;
    private final Map<Integer, PartitionStats> partitions;
    private final double balanceScore;
    private final Double skewRatio;
    private final Double stddevMessages;
    private final List<String> warnings;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PartitionStats {
        private final long messageCount;
        private final int sampledCount;
    }
}
