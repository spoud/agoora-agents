package io.spoud.agoora.agents.api.observers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AllResponseObserverTest {

  AllResponseObserver<String> allResponseObserver;

  @BeforeEach
  void setup() {
    allResponseObserver = new AllResponseObserver<>();
  }

  @Test
  void testLastReponseObserver() {
    assertThat(allResponseObserver.getResponses()).isEmpty();

    allResponseObserver.onNext("a");
    assertThat(allResponseObserver.getResponses()).containsExactly("a");

    allResponseObserver.onNext("b");
    allResponseObserver.onNext("c");
    allResponseObserver.onCompleted();
    assertThat(allResponseObserver.getResponses()).containsExactly("a", "b", "c");

    assertThat(allResponseObserver.getError()).isNull();
  }
}
