package io.spoud.agoora.agents.api.utils;

import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

@RequiredArgsConstructor
public class LazySingletonInstance<T> {
  private final Supplier<T> generator;
  private T instance;

  public boolean isInstantiated() {
    return instance != null;
  }

  public synchronized T getInstance() {
    if (instance == null) {
      instance = generator.get();
    }
    return instance;
  }
}
