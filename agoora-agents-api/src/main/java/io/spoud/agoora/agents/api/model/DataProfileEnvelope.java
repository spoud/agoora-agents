package io.spoud.agoora.agents.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataProfileEnvelope {
    private final String version;

    @JsonRawValue
    private final String valueProfile;

    @JsonRawValue
    private final String keyProfile;

    private final ValueEncoding valueEncoding;

    private final Map<String, Object> sourceMetadata;

    public static DataProfileEnvelope wrap(String profileJson) {
        return DataProfileEnvelope.builder()
                .version("3")
                .valueProfile(profileJson)
                .build();
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValueEncoding {
        private final String charset;
        private final double confidence;
    }
}
