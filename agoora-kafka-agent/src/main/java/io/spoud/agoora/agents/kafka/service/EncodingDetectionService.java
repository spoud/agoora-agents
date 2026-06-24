package io.spoud.agoora.agents.kafka.service;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@ApplicationScoped
public class EncodingDetectionService {

    private static final int MAX_DETECT_BYTES = 8192;

    @Data
    @Builder
    public static class EncodingResult {
        private final String charset;
        private final double confidence;
    }

    public EncodingResult detectEncoding(List<byte[]> rawSamples) {
        if (rawSamples == null || rawSamples.isEmpty()) {
            return EncodingResult.builder().charset("UNKNOWN").confidence(0.0).build();
        }

        byte[] combined = combineBytes(rawSamples, MAX_DETECT_BYTES);

        try {
            CharsetDetector detector = new CharsetDetector();
            detector.setText(combined);
            CharsetMatch match = detector.detect();

            if (match != null) {
                return EncodingResult.builder()
                        .charset(match.getName())
                        .confidence(match.getConfidence() / 100.0)
                        .build();
            }
        } catch (Exception e) {
            LOG.warn("Encoding detection failed: {}", e.getMessage());
        }

        return EncodingResult.builder().charset("UNKNOWN").confidence(0.0).build();
    }

    private byte[] combineBytes(List<byte[]> samples, int maxBytes) {
        int totalSize = 0;
        for (byte[] sample : samples) {
            totalSize += sample.length;
            if (totalSize >= maxBytes) break;
        }
        totalSize = Math.min(totalSize, maxBytes);

        byte[] combined = new byte[totalSize];
        int offset = 0;
        for (byte[] sample : samples) {
            int toCopy = Math.min(sample.length, totalSize - offset);
            System.arraycopy(sample, 0, combined, offset, toCopy);
            offset += toCopy;
            if (offset >= totalSize) break;
        }
        return combined;
    }
}
