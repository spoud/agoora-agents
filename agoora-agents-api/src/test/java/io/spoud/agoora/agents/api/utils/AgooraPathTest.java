package io.spoud.agoora.agents.api.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgooraPathTest {

  @Test
  void testNormalParse() {
    final AgooraPath parse = AgooraPath.parse("/org/rg/name");
    assertThat(parse.getResourceGroupPath()).isEqualTo("/org/rg/");
    assertThat(parse.getName()).isEqualTo("name");
  }

  @Test
  void testGetAbsolutePath() {
    final AgooraPath parse = AgooraPath.parse("/org/rg/name");
    assertThat(parse.getAbsolutePath()).isEqualTo("/org/rg/name");
    assertThat(parse).hasToString("/org/rg/name");
  }

  @Test
  void testNormalOrg() {
    final AgooraPath parse = AgooraPath.parse("/org/name");
    assertThat(parse.getResourceGroupPath()).isEqualTo("/org/");
    assertThat(parse.getName()).isEqualTo("name");
  }

  @Test
  void testWrongPath() {
    assertThatThrownBy(() -> AgooraPath.parse("/org/rg/"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid path '/org/rg/'");
  }

  @Test
  void testNoPath() {
    assertThatThrownBy(() -> AgooraPath.parse("asdfsdf"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid path 'asdfsdf'");
  }

  @Test
  void testOrganization() {
    assertThatThrownBy(() -> AgooraPath.parse("/asdfsdfsd"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid path '/asdfsdfsd'");
  }
}
