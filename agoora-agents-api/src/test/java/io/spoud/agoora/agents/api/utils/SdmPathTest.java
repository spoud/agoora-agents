package io.spoud.agoora.agents.api.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SdmPathTest {

  @Test
  void testNormalParse() {
    final SdmPath parse = SdmPath.parse("/org/rg/name");
    assertThat(parse.getResourceGroupPath()).isEqualTo("/org/rg/");
    assertThat(parse.getName()).isEqualTo("name");
  }

  @Test
  void testGetAbsolutePath() {
    final SdmPath parse = SdmPath.parse("/org/rg/name");
    assertThat(parse.getAbsolutePath()).isEqualTo("/org/rg/name");
    assertThat(parse).hasToString("/org/rg/name");
  }

  @Test
  void testNormalOrg() {
    final SdmPath parse = SdmPath.parse("/org/name");
    assertThat(parse.getResourceGroupPath()).isEqualTo("/org/");
    assertThat(parse.getName()).isEqualTo("name");
  }

  @Test
  void testWrongPath() {
    assertThatThrownBy(() -> SdmPath.parse("/org/rg/"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid path '/org/rg/'");
  }

  @Test
  void testNoPath() {
    assertThatThrownBy(() -> SdmPath.parse("asdfsdf"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid path 'asdfsdf'");
  }

  @Test
  void testOrganization() {
    assertThatThrownBy(() -> SdmPath.parse("/asdfsdfsd"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid path '/asdfsdfsd'");
  }
}
