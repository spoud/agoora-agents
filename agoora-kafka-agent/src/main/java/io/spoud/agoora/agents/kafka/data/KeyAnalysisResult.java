package io.spoud.agoora.agents.kafka.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeyAnalysisResult {
    private final String keyFormat;
    private final double presenceRate;
    private final int totalRecords;
    private final int keysPresent;
    private final int uniqueCount;
    private final Long minByteSize;
    private final Long maxByteSize;
    private final Double avgByteSize;
    private final List<String> sampleValues;
    private final String fullProfileJson;
}
