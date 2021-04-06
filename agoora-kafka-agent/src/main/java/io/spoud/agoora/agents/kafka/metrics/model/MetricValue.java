package io.spoud.agoora.agents.kafka.metrics.model;

import lombok.Data;

@Data
public class MetricValue {
  private double timestamp;
  private double value;

  public MetricValue(double timestamp, double value) {
    this.timestamp = timestamp;
    this.value = value;
  }
}
