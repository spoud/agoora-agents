package io.spoud.agoora.agents.api.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SdmPathConverterTest {

    @Test
    void testConverter(){
        final SdmPath convert = new SdmPathConverter().convert("/a/b");
        assertThat(convert.getAbsolutePath()).isEqualTo("/a/b");
        assertThat(convert.getResourceGroupPath()).isEqualTo("/a/");
        assertThat(convert.getName()).isEqualTo("b");
    }

}
