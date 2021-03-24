package io.spoud.agoora.agents.api.observers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class LastResponseObserverTest {

  LastResponseObserver<String> lastResponseObserver;

  @BeforeEach
  void setup() {
    lastResponseObserver = new LastResponseObserver<>();
  }

  @Test
  void testLastReponseObserver() {
    assertThat(lastResponseObserver.getLastResponse()).isNull();

    lastResponseObserver.onNext("a");
    assertThat(lastResponseObserver.getLastResponse()).isEqualTo("a");

    lastResponseObserver.onNext("b");
    lastResponseObserver.onNext("c");
    lastResponseObserver.onCompleted();
    assertThat(lastResponseObserver.getLastResponse()).isEqualTo("c");

    assertThat(lastResponseObserver.getError()).isNull();
  }

  @Test
  void testError() {
    assertThat(lastResponseObserver.getError()).isNull();
    lastResponseObserver.onError(new RuntimeException("a"));
    assertThat(lastResponseObserver.getError())
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("a");
    lastResponseObserver.onError(new RuntimeException("b"));
    assertThat(lastResponseObserver.getError())
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("b");
  }

  @Timeout(10)
  @Test
  void awaitCompletion() throws InterruptedException {
    lastResponseObserver.onNext("a");

    Executors.newSingleThreadScheduledExecutor()
        .schedule(lastResponseObserver::onCompleted, 1, TimeUnit.SECONDS);

    lastResponseObserver.awaitCompletion();
  }
}
