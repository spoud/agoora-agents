package io.spoud.agoora.agents.mqtt.mqtt;

import io.spoud.agoora.agents.mqtt.data.TopicDescription;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class IterationContextTest {

  IterationContext iterationContext;

  @BeforeEach
  void setup() {
    iterationContext = new IterationContext(10, Duration.ofSeconds(1));
  }

  @Test
  void testRetainedWaitingTime() throws InterruptedException {
    final TopicDescription topicDescription =
        TopicDescription.builder().dataPortTopic("/blabla/sadfsdf").build();
    iterationContext.countMessage(topicDescription, createMqttMessage(true, 1));
    iterationContext.countMessage(topicDescription, createMqttMessage(false, 2));
    iterationContext.countMessage(topicDescription, createMqttMessage(true, 3));

    assertThat(iterationContext.getMessagesCount().get(topicDescription).getMessages().get())
        .isEqualTo(1);
    assertThat(iterationContext.getMessagesCount().get(topicDescription).getBytes().get())
        .isEqualTo(2);

    Thread.sleep(1000);

    iterationContext.countMessage(topicDescription, createMqttMessage(true, 4));
    iterationContext.countMessage(topicDescription, createMqttMessage(false, 5));
    iterationContext.countMessage(topicDescription, createMqttMessage(true, 6));

    assertThat(iterationContext.getMessagesCount().get(topicDescription).getMessages().get())
        .isEqualTo(4);
    assertThat(iterationContext.getMessagesCount().get(topicDescription).getBytes().get())
        .isEqualTo(17);
  }

  MqttMessage createMqttMessage(boolean retained, int messageSize) {
    final MqttMessage mqttMessage = new MqttMessage(new byte[messageSize]);
    mqttMessage.setRetained(retained);
    return mqttMessage;
  }
}
