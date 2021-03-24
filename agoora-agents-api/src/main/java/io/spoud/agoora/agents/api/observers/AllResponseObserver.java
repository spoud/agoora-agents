package io.spoud.agoora.agents.api.observers;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class AllResponseObserver<T> extends AbstractResponseObserver<T> {
  @Getter private List<T> responses = Collections.synchronizedList(new ArrayList<>());

  @Override
  protected void accumulator(T response) {
    responses.add(response);
  }
}
