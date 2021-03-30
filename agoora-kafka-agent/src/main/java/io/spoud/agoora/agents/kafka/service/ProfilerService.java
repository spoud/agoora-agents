package io.spoud.agoora.agents.kafka.service;

import io.spoud.agoora.agents.kafka.kafka.KafkaTopicReader;
import io.spoud.agoora.agents.kafka.repository.KafkaTopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ProfilerService {
  private final KafkaTopicRepository kafkaTopicRepository;
  private final KafkaTopicReader kafkaTopicReader;

  public void profileData() {
    kafkaTopicRepository
        .getStates()
        .forEach(
            kafkaTopic -> {
              final List<byte[]> samples = kafkaTopicReader.getSamples(kafkaTopic.getTopicName());
            });
  }
}
