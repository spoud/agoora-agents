package io.spoud.agoora.agents.kafka.config.data;

import java.time.Duration;
import java.util.Optional;

public interface SchemaCacheConfig {

    Optional<Long> SchemaById();
    Optional<Duration> schemaExpiration();
    Optional<Long> topicNameSchemaIdCache();
    Optional<Duration> topicNameIdExpiration();

}
