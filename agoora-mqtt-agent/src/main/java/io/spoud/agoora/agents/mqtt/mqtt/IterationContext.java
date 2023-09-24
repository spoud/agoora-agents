package io.spoud.agoora.agents.mqtt.mqtt;

import io.spoud.agoora.agents.api.map.MonitoredConcurrentHashMap;
import io.spoud.agoora.agents.mqtt.data.TopicDescription;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
public class IterationContext {

  private final int maxBuffer;
  private final Duration waitTimeBeforeCountingRetained;
  private final Instant startTime = Instant.now();

  private final Map<TopicDescription, List<MqttMessage>> messagesBuffer = new MonitoredConcurrentHashMap<>("messages_buffer", IterationContext.class);
  private final Map<TopicDescription, Instant> firstMessageInstant = new MonitoredConcurrentHashMap<>("first_message_instant", IterationContext.class);
  private final Map<TopicDescription, MessageCounter> countMessage = new MonitoredConcurrentHashMap<>("count_message", IterationContext.class);

  /** Return true if it's new */
  public synchronized boolean storeMessage(TopicDescription topic, MqttMessage message) {
    firstMessageInstant.computeIfAbsent(topic, t -> Instant.now());
    final List<MqttMessage> list =
        messagesBuffer.computeIfAbsent(
            topic, t -> Collections.synchronizedList(new LinkedList<>()));
    list.add(message);
    return list.size() == 1;
  }

  public synchronized List<MqttMessage> messageForTopic(TopicDescription topic) {
    return Collections.unmodifiableList(messagesBuffer.get(topic));
  }

  public synchronized boolean reachedMaxBuffer(TopicDescription topic) {
    final List<MqttMessage> list = messagesBuffer.get(topic);
    return list != null && list.size() >= maxBuffer;
  }

  public synchronized void countMessage(TopicDescription topic, MqttMessage message) {
    countMessage.computeIfAbsent(topic, t -> new MessageCounter());
    if (!message.isRetained()
        || Duration.between(startTime, Instant.now()).compareTo(waitTimeBeforeCountingRetained)
            > 0) {
      countMessage.get(topic).getMessages().incrementAndGet();
      countMessage.get(topic).getBytes().addAndGet(message.getPayload().length);
    }
  }

  public Map<TopicDescription, MessageCounter> getMessagesCount() {
    return countMessage;
  }

  @Getter
  public static class MessageCounter {
    private final AtomicLong messages = new AtomicLong(0L);
    private final AtomicLong bytes = new AtomicLong(0L);
  }
}
