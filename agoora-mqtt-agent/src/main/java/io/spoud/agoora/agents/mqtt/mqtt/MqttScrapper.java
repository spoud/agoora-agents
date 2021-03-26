package io.spoud.agoora.agents.mqtt.mqtt;

import io.spoud.agoora.agents.mqtt.config.data.MqttAgooraConfig;
import io.spoud.agoora.agents.mqtt.config.data.ScrapperConfig;
import io.spoud.agoora.agents.mqtt.data.TopicDescription;
import io.spoud.agoora.agents.mqtt.service.DataService;
import io.spoud.agoora.agents.mqtt.service.ProfilerService;
import io.spoud.agoora.agents.mqtt.utils.MqttUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.enterprise.context.ApplicationScoped;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class MqttScrapper {

  private final MqttAgooraConfig config;
  private final MqttClient mqttClient;
  private final DataService dataService;
  private final ProfilerService profilerService;

  private void mqttListener(
      IterationContext context, String basePath, String topic, MqttMessage msg) {
    MqttUtil.extractDataPortFromTopic(basePath, topic)
        .ifPresentOrElse(
            topicDescription -> {
              LOG.trace(
                  "Message received for data port '{}' and topic '{}': {}",
                  topicDescription,
                  topic,
                  msg);

              context.countMessage(topicDescription, msg);

              if (!context.reachedMaxBuffer(topicDescription)) {
                // we still have message to read
                final boolean isNew = context.storeMessage(topicDescription, msg);
                if (isNew) {
                  // this is the first time we receive something from this topic
                  dataService.updateStates(topicDescription);
                }
                if (context.reachedMaxBuffer(topicDescription)) {
                  // we've receive enough let's profile and send statistics

                  if (config.getScrapper().getProfiling().isEnabled()) {
                    profilerService.profileMqttMessages(
                        topicDescription, context.messageForTopic(topicDescription));
                  }
                }
              }
            },
            () -> {
              LOG.warn(
                  "Topic '{}' was discarded because we couldn't extract a data port name", topic);
            });
  }

  public IterationContext startIteration() {
    IterationContext context = new IterationContext(config.getScrapper().getMaxSamples(), config.getScrapper().getWaitTimeBeforeCountingRetained());
    getPaths()
        .forEach(
            path -> {
              try {
                final String mqttQuery = getMqttQueryFromPath(path);
                LOG.info("Subscribing to '{}'", mqttQuery);
                mqttClient.subscribe(
                    mqttQuery, (topic, msg) -> mqttListener(context, path, topic, msg));
              } catch (MqttException e) {
                LOG.error("Unable to subscribe to path {}", path, e);
              }
            });
    return context;
  }

  public void stopRemainingOfPreviousIteration(IterationContext context) {
    final ScrapperConfig scrapperConfig = config.getScrapper();
    final double ratio =
        1.0 * scrapperConfig.getPeriod().toMillis() / scrapperConfig.getMaxWait().toMillis();

    if (context != null) {
      context
          .getMessagesCount()
          .entrySet()
          .forEach(
              entry -> {
                sendStatictics(entry.getKey(), entry.getValue(), ratio);
              });
    }
    getPaths()
        .forEach(
            path -> {
              try {
                final String mqttQuery = getMqttQueryFromPath(path);
                LOG.info("Unsubscribing to '{}'", mqttQuery);
                mqttClient.unsubscribe(mqttQuery);
              } catch (MqttException e) {
                LOG.error("Unable to unsubscribe to path {}", path);
              }
            });
  }

  private Stream<String> getPaths() {
    return Stream.of(config.getMqtt().getPaths().split(","))
        .map(p -> p.endsWith("/") ? p : p + "/");
  }

  private String getMqttQueryFromPath(String path) {
    return path + "#";
  }

  private void sendStatictics(
          TopicDescription topicDescription, IterationContext.MessageCounter counter, double ratio) {
    dataService.updateMetrics(
        topicDescription, counter.getMessages().get() * ratio, counter.getBytes().get() * ratio);
  }
}
