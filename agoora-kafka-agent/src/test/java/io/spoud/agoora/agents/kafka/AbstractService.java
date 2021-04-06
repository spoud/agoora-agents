package io.spoud.agoora.agents.kafka;

import io.quarkus.test.common.QuarkusTestResource;
import io.spoud.agoora.agents.kafka.config.KafkaResource;
import io.spoud.agoora.agents.kafka.config.SchemaRegistryResource;

@QuarkusTestResource(KafkaResource.class)
@QuarkusTestResource(SchemaRegistryResource.class)
public class AbstractService {}
