package io.spoud.agoora.agents.api.metrics;

import com.sun.management.OperatingSystemMXBean;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.spoud.agoora.agents.api.client.MetricsClient;
import io.spoud.sdm.looker.domain.v1alpha1.ResourceMetricType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RegisterForReflection(targets = {OperatingSystemMXBean.class})
public class OperationalMetricsService {
  private final MetricsClient metricsClient;

  private Instant lastIterationStart;
  private long loopCounter;

  public synchronized void iterationStart() {
    lastIterationStart = Instant.now();
  }

  public synchronized void iterationEnd(
      String agentUsername, String transportPath, Duration loopInterval) {
    if (lastIterationStart == null) {
      throw new IllegalStateException("You should call iterationStart before iterationEnd");
    }
    loopCounter++;

    final Duration actualDuration = Duration.between(lastIterationStart, Instant.now());
    double percentLoop = 100.0 * actualDuration.toMillis() / loopInterval.toMillis();
    lastIterationStart = null;

    List<MetricsClient.OperationalMetric> metrics = new ArrayList<>();

    addProcessMetrics(metrics);

    metrics.add(
        new MetricsClient.OperationalMetric(
            ResourceMetricType.Type.LOOP_TIME_MS, actualDuration.toMillis()));
    metrics.add(
        new MetricsClient.OperationalMetric(ResourceMetricType.Type.LOOP_PERCENT, percentLoop));
    metrics.add(
        new MetricsClient.OperationalMetric(ResourceMetricType.Type.LOOPS_COUNTER, loopCounter));

    metricsClient.updateOperationMetrics(agentUsername, transportPath, metrics);
    LOG.info("Iteration took {}", actualDuration);
  }

  protected void addProcessMetrics(List<MetricsClient.OperationalMetric> metrics) {
    MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    final long memoryUsed = heapMemoryUsage.getUsed();

    metrics.add(
        new MetricsClient.OperationalMetric(
            ResourceMetricType.Type.MEMORY_USAGE_BYTES, memoryUsed));

    double processCpuLoad = -1;

    // DISABLED for now because of: https://github.com/oracle/graal/issues/3289
    //      OperatingSystemMXBean operatingSystemMXBean =
    //          ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    //      processCpuLoad = operatingSystemMXBean.getProcessCpuLoad();
    //
    //      metrics.add(
    //          new MetricsClient.OperationalMetric(
    //              ResourceMetricType.Type.CPU_USAGE_PERCENT, processCpuLoad * 100));

    final long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
    metrics.add(
        new MetricsClient.OperationalMetric(ResourceMetricType.Type.AGENT_UPTIME_MS, uptimeMs));

    LOG.debug(
        "Uptime: {}, CPU load: {}, memory usage: {}",
        Duration.ofMillis(uptimeMs),
        processCpuLoad,
        memoryUsed);
  }
}
