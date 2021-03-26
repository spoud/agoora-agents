package io.spoud.agoora.agents.mqtt.repository;

import com.google.common.util.concurrent.AtomicDouble;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class MetricsRepository {
  private final Map<String, AtomicDouble> messageCounter = new HashMap<>();

  public double addMessagesForDurationAndReturnCounter(String dataPortId, double messagePerDuration) {
    messageCounter.compute(
        dataPortId,
        (s, atomicLong) -> {
          if (atomicLong == null) {
            return new AtomicDouble(messagePerDuration);
          } else {
            atomicLong.addAndGet(messagePerDuration);
            return atomicLong;
          }
        });
    return messageCounter.get(dataPortId).get();
  }
}
