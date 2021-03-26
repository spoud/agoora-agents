package io.spoud.agoora.agents.kafka.schema.confluent;

import io.spoud.agoora.agents.kafka.config.data.RegistryConfluentConfig;
import io.spoud.agoora.agents.kafka.schema.SchemaRegistryClient;
import io.spoud.sdm.schema.domain.v1alpha.Schema;
import io.spoud.sdm.schema.domain.v1alpha.SchemaEncoding;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.WebApplicationException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class ConfluentSchemaRegistry implements SchemaRegistryClient {

  // FIXME for the key we have to wait for the support of multiple schemas
  public static final List<String> SUBJECTS_NAME =
      Arrays.asList(
          //    "key",
          "value");
  public static final String DEEP_DIVE_TOOL_SUBJECT = "value";

  // The subject postfix is either "key" or "value"
  public static final String PUBLIC_URL_SUBJECT_POSTFIX_REPLACEMENT = "{SUBJECT_POSTFIX}";

  // This is the topic name
  public static final String PUBLIC_URL_TOPIC_REPLACEMENT = "{TOPIC}";

  // This is the full subject. It's the same as "{TOPIC}-{SUBJECT_POSTFIX}"
  public static final String PUBLIC_URL_SUBJECT_REPLACEMENT = "{SUBJECT}";

  private final RegistryConfluentConfig config;
  private final Optional<String> publicUrl;
  private final ConfluentRegistryResource confluentRegistryResource;

  public ConfluentSchemaRegistry(
      ConfluentRegistryResource confluentRegistryResource, RegistryConfluentConfig config) {
    this.confluentRegistryResource = confluentRegistryResource;

    this.config = config;
    this.publicUrl = config.getPublicUrl();
  }

  @Override
  public List<Schema> getNewSchemaForTopic(String topic) {
    LOG.debug("Searching for schema for topic '{}'", topic);
    return SUBJECTS_NAME.stream()
        .map(s -> getSchema(topic, s))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(this::mapToSchemaObject)
        .collect(Collectors.toList());
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

  private Optional<String> getSchema(String topic, String type) {
    LOG.debug("Looking for schema for topic '{}' and type '{}'", topic, type);

    try {
      final SchemaRegistrySubject latestSubject =
          confluentRegistryResource.getLatestSubject(topic, type);
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
