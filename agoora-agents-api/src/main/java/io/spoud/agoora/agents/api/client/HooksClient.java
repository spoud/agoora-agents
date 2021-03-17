package io.spoud.agoora.agents.api.client;

import io.grpc.stub.StreamObserver;
import io.spoud.sdm.hooks.domain.v1.DataItemFilter;
import io.spoud.sdm.hooks.domain.v1.DataPortFilter;
import io.spoud.sdm.hooks.domain.v1.DataSubscriptionStateFilter;
import io.spoud.sdm.hooks.domain.v1.LogRecord;
import io.spoud.sdm.hooks.domain.v1.PositionPreset;
import io.spoud.sdm.hooks.domain.v1.StateChangeFilter;
import io.spoud.sdm.hooks.domain.v1.StateChangeStartPosition;
import io.spoud.sdm.hooks.v1.StateChangerGrpc;
import io.spoud.sdm.hooks.v1.SubscribeChangeRequest;
import io.spoud.sdm.hooks.v1.SubscribeChangeResponse;
import io.spoud.sdm.global.selection.v1.BaseRef;
import io.spoud.sdm.global.selection.v1.IdPathRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class HooksClient {

  private final StateChangerGrpc.StateChangerStub stateChangerStub;

  public AutoCloseable startListening(Consumer<LogRecord> callback) {
    return startListening(
        callback,
        StateChangeFilter.newBuilder()
            .setDataPortFilter(DataPortFilter.newBuilder().build())
            .build(),
        StateChangeFilter.newBuilder()
            .setDataSubscriptionStateFilter(DataSubscriptionStateFilter.newBuilder().build())
            .build());
  }

  public AutoCloseable startListening(
      Consumer<LogRecord> callback,
      String transportAbsolutePath,
      boolean dataPort,
      boolean dataItem,
      boolean dataSubscriptionState) {
    List<StateChangeFilter> filters = new ArrayList<>(3);
    final BaseRef transportRef =
        BaseRef.newBuilder()
            .setIdPath(IdPathRef.newBuilder().setPath(transportAbsolutePath).build())
            .build();
    if (dataPort) {
      filters.add(
          StateChangeFilter.newBuilder()
              .setDataPortFilter(
                  DataPortFilter.newBuilder()
                      .setTransportRef(
                        transportRef)
                      .build())
              .build());
    }
    if (dataItem) {
      filters.add(
          StateChangeFilter.newBuilder()
              .setDataItemFilter(
                  DataItemFilter.newBuilder()
                      .setTransportRef(
                        transportRef)
                      .build())
              .build());
    }
    if (dataSubscriptionState) {
      filters.add(
          StateChangeFilter.newBuilder()
              .setDataSubscriptionStateFilter(
                  DataSubscriptionStateFilter.newBuilder()
                      .setTransportRef(
                        transportRef)
                      .build())
              .build());
    }

    if(filters.isEmpty()){
      throw new IllegalArgumentException("You should at least subscribe to one type of entity");
    }
    return startListening(callback, filters.toArray(new StateChangeFilter[0]));
  }

  public AutoCloseable startListening(Consumer<LogRecord> callback, StateChangeFilter... filters) {

    LOG.info("Starting hooks client");

    final HooksStreamObserver hooksStreamObserver =
        new HooksStreamObserver(Arrays.asList(filters), callback, stateChangerStub);
    hooksStreamObserver.connectToHooks();
    return hooksStreamObserver;
  }

  @RequiredArgsConstructor
  protected static class HooksStreamObserver
      implements StreamObserver<SubscribeChangeResponse>, AutoCloseable {

    private static final Duration RETRY_TIME_INCREMENT = Duration.ofSeconds(5);
    private static final Duration RETRY_MAX_TIME = Duration.ofSeconds(60);
    public final List<StateChangeFilter> filters;
    public final Consumer<LogRecord> callback;
    private final StateChangerGrpc.StateChangerStub stateChangerStub;
    private final AtomicReference<String> lastCursor = new AtomicReference<>(null);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private int retriesCounter = 0;

    public synchronized void connectToHooks() {
      while (running.get()) {
        waitBeforeNextTry();
        retriesCounter++;
        try {
          stateChangerStub.subscribeChange(buildRequest(), this);
          return;
        } catch (Exception e) {
          LOG.info("Could not connect to hooks. Will retry.");
        }
      }
      LOG.info("Closing connection");
    }

    @Override
    public void onNext(SubscribeChangeResponse event) {
      if (running.get()) {
        retriesCounter = 0;
        if (event.hasLogRecord()) {
          LOG.trace("Log record received: {}", event.getLogRecord());
          lastCursor.set(event.getLogRecord().getCursor());
          callback.accept(event.getLogRecord());
        }
        if (event.hasStreamInfo()) {
          LOG.trace("Stream info: {}", event.getStreamInfo().getTimestamp().getSeconds());
        }
      }
    }

    @Override
    public void onError(Throwable t) {
      // ignore jwt expired error
      if (t.getMessage().contains("JWT is expired")) {
        LOG.debug("JWT is expired, reconnecting to hooks with cursor '{}'", lastCursor.get());
      } else {
        LOG.warn("Reconnect due to hooks stream error: ", t);
      }
      connectToHooks();
    }

    @Override
    public void onCompleted() {
      LOG.warn("Reconnect due to hooks server closed the connection.");
      connectToHooks();
    }

    @Override
    public void close() throws Exception {
      running.set(false);
    }

    protected void waitBeforeNextTry() {
      try {
        long ms =
            Math.min(retriesCounter * RETRY_TIME_INCREMENT.toMillis(), RETRY_MAX_TIME.toMillis());
        LOG.trace("Waiting {} ms before next try", ms);
        Thread.sleep(ms);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    protected SubscribeChangeRequest buildRequest() {
      final String cursor = lastCursor.get();
      final StateChangeStartPosition.Builder startPositionBuilder =
          StateChangeStartPosition.newBuilder();
      if (cursor != null && !cursor.isBlank()) {
        startPositionBuilder.setCursor(cursor);
      } else {
        startPositionBuilder.setPreset(PositionPreset.EARLIEST);
      }

      return SubscribeChangeRequest.newBuilder()
          .addAllFilters(filters)
          .setStartPosition(startPositionBuilder.build())
          .build();
    }
  }
}
