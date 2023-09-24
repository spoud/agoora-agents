package io.spoud.agoora.agents.kafka.schema.confluent;

import io.quarkus.logging.Log;
import io.spoud.agoora.agents.api.map.MonitoredConcurrentHashMap;
import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import io.spoud.agoora.agents.kafka.schema.SchemaRegistryClient;
import io.spoud.sdm.schema.domain.v1alpha.Schema;
import io.spoud.sdm.schema.domain.v1alpha.SchemaEncoding;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Map;
import java.util.Optional;

@Slf4j
@ApplicationScoped
public class ConfluentSchemaRegistry implements SchemaRegistryClient {

  public static final String DEEP_DIVE_TOOL_SUBJECT = "value";

  // The subject postfix is either "key" or "value"
  public static final String PUBLIC_URL_SUBJECT_POSTFIX_REPLACEMENT = "{SUBJECT_POSTFIX}";

  // This is the topic name
  public static final String PUBLIC_URL_TOPIC_REPLACEMENT = "{TOPIC}";

  // This is the full subject. It's the same as "{TOPIC}-{SUBJECT_POSTFIX}"
  public static final String PUBLIC_URL_SUBJECT_REPLACEMENT = "{SUBJECT}";

  private final Optional<String> publicUrl;

  @Inject
  @RestClient
  Instance<ConfluentRegistrySubjectResource> confluentRegistrySubjectResource;
  @Inject
  @RestClient
  Instance<ConfluentRegistrySchemaResource> confluentRegistrySchemaResource;

  @ConfigProperty(name = "rest-confluent-registry/mp-rest/url")
  Optional<String> registryUrl;

  private Map<Long, SchemaRegistrySubject> schemaByIdCache = new MonitoredConcurrentHashMap<>("schema_by_id_cache", ConfluentSchemaRegistry.class);

  public ConfluentSchemaRegistry(KafkaAgentConfig config) {
    this.publicUrl = config.registry().confluent().publicUrl();
  }

  private boolean registryDefined() {
    if (registryUrl.filter(StringUtils::isNotBlank).isPresent()) {
      return true;
    }
    LOG.trace("No schema registry url defined");
    return false;
  }

  @Override
  public Optional<Schema> getLatestSchemaForTopic(String topic, KafkaStreamPart part) {
    if (!registryDefined()) {
      return Optional.empty();
    }
    LOG.debug("Searching for {} schema for topic '{}'", part, topic);
    return getSchema(topic, part).map(this::mapToSchemaObject);
  }

  public Optional<SchemaRegistrySubject> getSchemaById(long id) {
    if (!registryDefined()) {
      return Optional.empty();
    }
    final SchemaRegistrySubject cachedValue = schemaByIdCache.get(id);
    if (cachedValue != null) {
      return Optional.of(cachedValue);
    }
    try {
      final SchemaRegistrySubject schema = confluentRegistrySchemaResource.get().getById(id);
      schemaByIdCache.put(id, schema);
      return Optional.of(schema);
    } catch (WebApplicationException ex) {

      if (ex.getResponse().getStatus() == 404) {
        // not issue, the schema just doesn't exists
      } else {
        LOG.error("Exception when querying schema registry for url", ex);
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<String> getDeepDiveToolUrl(String topic) {
    return publicUrl
        .filter(StringUtils::isNotBlank)
        .map(
            url ->
                url.replace(PUBLIC_URL_SUBJECT_POSTFIX_REPLACEMENT, DEEP_DIVE_TOOL_SUBJECT)
                    .replace(PUBLIC_URL_TOPIC_REPLACEMENT, topic)
                    .replace(PUBLIC_URL_SUBJECT_REPLACEMENT, topic + "-" + DEEP_DIVE_TOOL_SUBJECT));
  }

  private Schema mapToSchemaObject(SchemaRegistrySubject rawSchema) {
    SchemaEncoding.Type encoding = SchemaEncoding.Type.AVRO; // default encoding for confluent
    try {
      if (rawSchema.getSchemaType() != null) {
        encoding = SchemaEncoding.Type.valueOf(rawSchema.getSchemaType());
      }
    } catch (IllegalArgumentException e) {
      Log.error(e);
    }
    return Schema.newBuilder()
        .setEncoding(encoding)
        .setContent(rawSchema.getSchema())
        .build();
  }

  // TODO cache, but timed
  private Optional<SchemaRegistrySubject> getSchema(String topic, KafkaStreamPart part) {
    LOG.debug("Looking for schema for topic '{}' and type '{}'", topic, part);

    try {
      final SchemaRegistrySubject latestSubject =
          confluentRegistrySubjectResource
              .get()
              .getLatestSubject(topic + "-" + part.getSubjectPostfix());
      return Optional.of(latestSubject);
    } catch (WebApplicationException ex) {
      if (ex.getResponse().getStatus() == 404) {
        // not issue, the schema just doesn't exists
      } else {
        LOG.error("Exception when querying schema registry for url", ex);
      }
    } catch (ProcessingException ex) {
      LOG.error(
          "Issue with confluent schema registry, there is good chance that the certificate is not recognized",
          ex);
    }
    return Optional.empty();
  }
}
