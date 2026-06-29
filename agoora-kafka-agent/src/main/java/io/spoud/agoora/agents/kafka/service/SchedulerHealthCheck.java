package io.spoud.agoora.agents.kafka.service;

import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import java.time.Duration;
import java.time.Instant;

@Liveness
@RequiredArgsConstructor
public class SchedulerHealthCheck implements HealthCheck {

  private final CronService cronService;
  private final KafkaAgentConfig sdmConfig;

  @Override
  public HealthCheckResponse call() {
    Duration period = sdmConfig.scrapper().period();
    Instant lastTick = cronService.getLastTickTime();
    Duration silence = Duration.between(lastTick, Instant.now());

    if (silence.compareTo(period.multipliedBy(2)) > 0) {
      return HealthCheckResponse.named("Scheduler health check")
          .down()
          .withData("lastTickSecondsAgo", silence.toSeconds())
          .withData("maxAllowedSeconds", period.multipliedBy(2).toSeconds())
          .build();
    }
    return HealthCheckResponse.named("Scheduler health check").up().build();
  }
}
