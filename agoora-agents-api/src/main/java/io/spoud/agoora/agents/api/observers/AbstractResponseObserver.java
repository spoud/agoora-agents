package io.spoud.agoora.agents.api.observers;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

@Slf4j
public abstract class AbstractResponseObserver<T> implements StreamObserver<T> {
  private final CountDownLatch latch = new CountDownLatch(1);
  private Throwable error;

  public Throwable getError() {
    return this.error;
  }

  public void onNext(T t) {
    accumulator(t);
  }

  public void onError(Throwable t) {
    LOG.error("Error while handling the response observer : {}", t);
    this.error = t;
    this.latch.countDown();
  }

  public void onCompleted() {
    this.latch.countDown();
  }

  public void awaitCompletion() throws InterruptedException {
    this.latch.await();
  }

  protected abstract void accumulator(T response);
}
