package io.spoud.agoora.agents.api.observers;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LastResponseObserver<T> extends AbstractResponseObserver<T> {
  @Getter private T lastResponse;

  @Override
  protected void accumulator(T response) {
    lastResponse = response;
  }
}
