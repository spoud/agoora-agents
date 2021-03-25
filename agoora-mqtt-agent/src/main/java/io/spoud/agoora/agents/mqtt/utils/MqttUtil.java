package io.spoud.agoora.agents.mqtt.utils;

import io.spoud.agoora.agents.mqtt.data.TopicDescription;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@UtilityClass
public class MqttUtil {

  public static Optional<TopicDescription> extractDataPortFromTopic(String basePath, String topic) {
    if (!topic.startsWith(basePath)) {
      LOG.warn("Topic '{}' should start with '{}}'", topic, basePath);
      return Optional.empty();
    }

    String dataPort = topic.substring(basePath.length());
    if (dataPort.length() == 0) {
      // the base path is the topic, we cannot find a Topic
      return Optional.empty();
    }
    if (dataPort.charAt(0) == '/') {
      dataPort = dataPort.substring(1);
    }
    if (dataPort.length() == 0) {
      // the base path is the topic, we cannot find a Topic
      return Optional.empty();
    }

    final int nextSlashes = dataPort.indexOf('/');
    if (nextSlashes != -1) {
      dataPort = dataPort.substring(0, nextSlashes);
    }

    final String dataPortWithBase =
        basePath.endsWith("/") ? basePath + dataPort : basePath + "/" + dataPort;

    return Optional.of(TopicDescription.builder().dataPortTopic(dataPortWithBase).build());
  }
}
