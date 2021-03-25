package io.spoud.agoora.agents.mqtt.utils;

import io.spoud.agoora.agents.mqtt.data.TopicDescription;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MqttUtilTest {

  @Test
  void testValidPath() {
    assertThat(MqttUtil.extractDataPortFromTopic("/base/", "/base/item"))
        .map(TopicDescription::getDataPortTopic)
        .contains("/base/item");
    assertThat(MqttUtil.extractDataPortFromTopic("/base/", "/base/item/1"))
        .map(TopicDescription::getDataPortTopic)
        .contains("/base/item");
    assertThat(MqttUtil.extractDataPortFromTopic("/base", "/base/item"))
        .map(TopicDescription::getDataPortTopic)
        .contains("/base/item");
    assertThat(MqttUtil.extractDataPortFromTopic("/base", "/base/item/1"))
        .map(TopicDescription::getDataPortTopic)
        .contains("/base/item");
    assertThat(MqttUtil.extractDataPortFromTopic("/base", "/base/item/1/2"))
        .map(TopicDescription::getDataPortTopic)
        .contains("/base/item");
  }

  @Test
  void testNoItem() {
    assertThat(MqttUtil.extractDataPortFromTopic("/base/", "/base/")).isEmpty();
    assertThat(MqttUtil.extractDataPortFromTopic("/base", "/base")).isEmpty();
    assertThat(MqttUtil.extractDataPortFromTopic("/base", "/base/")).isEmpty();
  }

  @Test
  void testWrongTopic() {
    assertThat(MqttUtil.extractDataPortFromTopic("/base/", "/base")).isEmpty();
    assertThat(MqttUtil.extractDataPortFromTopic("/bla/", "/base")).isEmpty();
  }
}
