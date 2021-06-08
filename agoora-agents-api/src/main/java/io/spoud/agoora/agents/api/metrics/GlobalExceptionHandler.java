package io.spoud.agoora.agents.api.metrics;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

  private static final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

  static void setup() {

    Thread.setDefaultUncaughtExceptionHandler(globalExceptionHandler);
  }

  public void uncaughtException(Thread t, Throwable e) {
    LOG.error("Unhandled exception caught!", e);
  }
}
