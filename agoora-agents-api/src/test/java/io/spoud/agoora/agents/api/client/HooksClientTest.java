package io.spoud.agoora.agents.api.client;

import io.grpc.stub.StreamObserver;
import io.spoud.sdm.hooks.domain.v1.LogRecord;
import io.spoud.sdm.hooks.domain.v1.StateChangeFilter;
import io.spoud.sdm.hooks.v1.StateChangerGrpc;
import io.spoud.sdm.hooks.v1.SubscribeChangeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

class HooksClientTest {

  HooksClient hooksClient;
  StateChangerGrpc.StateChangerStub stub;

  @BeforeEach
  void setup() {
    stub = mock(StateChangerGrpc.StateChangerStub.class);
    hooksClient = new HooksClient(stub);
  }

  @Timeout(10)
  @Test
  void testHooksNoArgs() {
    ArgumentCaptor<StreamObserver<SubscribeChangeResponse>> captor =
        ArgumentCaptor.forClass(StreamObserver.class);
    String id = UUID.randomUUID().toString();
    doNothing().when(stub).subscribeChange(any(), captor.capture());

    List<LogRecord> allRecords = new ArrayList<>();
    final Consumer<LogRecord> consumer = allRecords::add;
    hooksClient.startListening(consumer);

    final StreamObserver<SubscribeChangeResponse> streamObserver = captor.getValue();

    streamObserver.onNext(
        SubscribeChangeResponse.newBuilder()
            .setLogRecord(LogRecord.newBuilder().setEntityUuid(id).build())
            .build());
    streamObserver.onCompleted();

    assertThat(allRecords).extracting(LogRecord::getEntityUuid).containsExactly(id);
  }

  @Timeout(10)
  @Test
  void testHooksBooleans() {
    ArgumentCaptor<StreamObserver<SubscribeChangeResponse>> captor =
        ArgumentCaptor.forClass(StreamObserver.class);
    String id = UUID.randomUUID().toString();
    doNothing().when(stub).subscribeChange(any(), captor.capture());

    List<LogRecord> allRecords = new ArrayList<>();
    final Consumer<LogRecord> consumer = allRecords::add;
    hooksClient.startListening(consumer, "/path/transport", true, true, true);

    final StreamObserver<SubscribeChangeResponse> streamObserver = captor.getValue();

    streamObserver.onNext(
        SubscribeChangeResponse.newBuilder()
            .setLogRecord(LogRecord.newBuilder().setEntityUuid(id).build())
            .build());
    streamObserver.onCompleted();

    assertThat(allRecords).extracting(LogRecord::getEntityUuid).containsExactly(id);
  }

  @Timeout(10)
  @Test
  void testHooksFilters() {
    ArgumentCaptor<StreamObserver<SubscribeChangeResponse>> captor =
        ArgumentCaptor.forClass(StreamObserver.class);
    String id = UUID.randomUUID().toString();
    doNothing().when(stub).subscribeChange(any(), captor.capture());

    List<LogRecord> allRecords = new ArrayList<>();
    final Consumer<LogRecord> consumer = allRecords::add;
    hooksClient.startListening(consumer, StateChangeFilter.newBuilder().build());

    final StreamObserver<SubscribeChangeResponse> streamObserver = captor.getValue();

    streamObserver.onNext(
        SubscribeChangeResponse.newBuilder()
            .setLogRecord(LogRecord.newBuilder().setEntityUuid(id).build())
            .build());
    streamObserver.onCompleted();

    assertThat(allRecords).extracting(LogRecord::getEntityUuid).containsExactly(id);
  }
}
