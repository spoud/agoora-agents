package io.spoud.agoora.agents.profiler.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProfilerGrpcServiceChunkingTest {

    @Test
    void singleChunk_whenContentFitsBudget() {
        List<String> chunks = ProfilerGrpcService.chunkUtf8("hello world", 1000);

        assertThat(chunks).containsExactly("hello world");
    }

    @Test
    void splitsOnByteBudget_pureAscii() {
        List<String> chunks = ProfilerGrpcService.chunkUtf8("abcdefgh", 3);

        assertThat(chunks).containsExactly("abc", "def", "gh");
    }

    @Test
    void emptyString_returnsEmptyList() {
        assertThat(ProfilerGrpcService.chunkUtf8("", 10)).isEmpty();
    }

    @Test
    void threeByteCharAtBoundary_notSplit() {
        // '€' (U+20AC) is 3 bytes in UTF-8; "ab" is 2 bytes, so appending '€' to a
        // 4-byte budget would overflow and must roll the char to the next chunk
        // instead of slicing its bytes in half.
        String content = "ab€cd";

        List<String> chunks = ProfilerGrpcService.chunkUtf8(content, 4);

        assertReassemblesLosslessly(content, chunks);
        assertNoChunkSplitsACharacter(chunks);
    }

    @Test
    void fourByteSurrogatePairAtBoundary_notSplit() {
        // an emoji outside the BMP is a UTF-16 surrogate pair (2 chars) encoding to
        // 4 UTF-8 bytes — codePointAt/charCount must treat it as one atomic unit.
        String emoji = "😀"; // 😀
        String content = "ab" + emoji + "cd";

        List<String> chunks = ProfilerGrpcService.chunkUtf8(content, 4);

        assertReassemblesLosslessly(content, chunks);
        assertNoChunkSplitsACharacter(chunks);
    }

    private void assertReassemblesLosslessly(String original, List<String> chunks) {
        assertThat(String.join("", chunks)).isEqualTo(original);
    }

    private void assertNoChunkSplitsACharacter(List<String> chunks) {
        for (String chunk : chunks) {
            byte[] bytes = chunk.getBytes(StandardCharsets.UTF_8);
            String roundTripped = new String(bytes, StandardCharsets.UTF_8);
            assertThat(roundTripped).isEqualTo(chunk);
            assertThat(roundTripped).doesNotContain("�");
        }
    }
}
