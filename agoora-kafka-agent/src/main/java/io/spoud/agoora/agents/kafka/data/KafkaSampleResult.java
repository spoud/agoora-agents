package io.spoud.agoora.agents.kafka.data;

import lombok.Builder;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class KafkaSampleResult {
    private final List<KafkaRecord> records;
    private final Map<Integer, PartitionRange> partitionRanges;

    @Data
    @Builder
    public static class KafkaRecord {
        private final byte[] value;
        private final byte[] key;
        private final int partition;
        private final long offset;
    }

    @Data
    @Builder
    public static class PartitionRange {
        private final long beginOffset;
        private final long endOffset;

        public long getMessageCount() {
            return endOffset - beginOffset;
        }
    }

    public List<byte[]> getValueBytes() {
        return records.stream()
                .map(KafkaRecord::getValue)
                .toList();
    }

    public List<byte[]> getKeyBytes() {
        return records.stream()
                .map(KafkaRecord::getKey)
                .filter(k -> k != null)
                .toList();
    }

    public static String toSafeString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8).replace("\u0000", "");
    }
}
