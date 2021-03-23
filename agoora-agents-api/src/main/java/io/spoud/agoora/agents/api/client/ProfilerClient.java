package io.spoud.agoora.agents.api.client;

import io.grpc.stub.StreamObserver;
import io.spoud.agoora.agents.api.observers.ProfileResponseObserver;
import io.spoud.sdm.profiler.service.v1alpha1.ProfileRequest;
import io.spoud.sdm.profiler.service.v1alpha1.ProfilerGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ProfilerClient {
  private final ProfilerGrpc.ProfilerStub stub;

  public ProfileResponseObserver.ProfilerResponse profileData(
      final String requestId, final List<byte[]> samples) {
    final ProfileResponseObserver responseObserver = new ProfileResponseObserver();
    final StreamObserver<ProfileRequest> profile = stub.profileDataStream(responseObserver);

    samples.stream()
        .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
        .map(v -> ProfileRequest.newBuilder().setRequestId(requestId).setJsonData(v).build())
        .forEach(profile::onNext);

    profile.onCompleted();
    try {
      responseObserver.awaitCompletion();
    } catch (InterruptedException ex) {
      LOG.error("Profiler interrupted");
      Thread.currentThread().interrupt();
    }

    return responseObserver.getResponse();
  }
}
