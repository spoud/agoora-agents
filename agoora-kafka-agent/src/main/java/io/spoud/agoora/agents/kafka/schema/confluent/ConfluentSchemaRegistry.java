package io.spoud.agoora.agents.kafka.schema.confluent;

import io.spoud.agoora.agents.kafka.config.data.KafkaAgentConfig;
import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import io.spoud.agoora.agents.kafka.schema.SchemaRegistryClient;
import io.spoud.sdm.schema.domain.v1alpha.Schema;
import io.spoud.sdm.schema.domain.v1alpha.SchemaEncoding;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
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

  @Inject @RestClient ConfluentRegistrySubjectResource confluentRegistrySubjectResource;
  @Inject @RestClient ConfluentRegistrySchemaResource confluentRegistrySchemaResource;

  public ConfluentSchemaRegistry(KafkaAgentConfig config) {
    this.publicUrl = config.getRegistry().getConfluent().getPublicUrl();
  }

  @Override
  public Optional<Schema> getLatestSchemaForTopic(String topic, KafkaStreamPart part) {
    LOG.debug("Searching for schema for topic '{}'", topic);
    return getSchema(topic, part).map(this::mapToSchemaObject);
  }

  public Optional<String> getSchemaById(long id) {
    try {
      return Optional.of(confluentRegistrySchemaResource.getById(id).getSchema());
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

  private Schema mapToSchemaObject(String rawSchema) {
    return Schema.newBuilder()
        .setEncodingValue(SchemaEncoding.Type.AVRO_VALUE)
        .setContent(rawSchema)
        .build();
  }

  private Optional<String> getSchema(String topic, KafkaStreamPart part) {
    LOG.debug("Looking for schema for topic '{}' and type '{}'", topic, part);

    try {
      final SchemaRegistrySubject latestSubject =
          confluentRegistrySubjectResource.getLatestSubject(topic, part);
      return Optional.of(latestSubject.getSchema());
    } catch (WebApplicationException ex) {

      if (ex.getResponse().getStatus() == 404) {
        // not issue, the schema just doesn't exists
      } else {
        LOG.error("Exception when querying schema registry for url", ex);
      }
    }
    return Optional.empty();
  }
}
