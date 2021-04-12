package io.spoud.agoora.agents.kafka.utils;

import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import io.spoud.agoora.agents.kafka.schema.confluent.ConfluentRegistrySubjectResource;
import io.spoud.agoora.agents.kafka.schema.confluent.SchemaRegistrySubject;
import lombok.SneakyThrows;
import org.apache.avro.Schema;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class SchemaRegistryUtil {

  private final ObjectMapper objectMapper = new ObjectMapper();
  @Inject @RestClient ConfluentRegistrySubjectResource confluentRegistrySubjectResource;

  @SneakyThrows
  public SchemaRegistrySubject addSchemaVersion(String topic, KafkaStreamPart part, String file) {
    try {

      String subject = part != null ? topic + "-" + part.getSubjectPostfix() : topic;
      String content =
          IOUtils.toString(
              SchemaRegistryUtil.class.getClassLoader().getResourceAsStream(file), "UTF-8");
      return confluentRegistrySubjectResource.addNewSchemaVersion(subject, content);
    } catch (WebApplicationException ex) {
      final ByteArrayInputStream entity = (ByteArrayInputStream) ex.getResponse().getEntity();
      final String message = IOUtils.toString(entity, StandardCharsets.UTF_8);
      throw new IllegalStateException(
          "Unable to process schema from file " + file + ". Reason: " + message, ex);
    }
  }

  @SneakyThrows
  public Schema getSchemaFromFile(String file) {
    final SchemaRegistrySubject subject =
        objectMapper.readValue(ResourceUtil.getFile(file), SchemaRegistrySubject.class);
    return new Schema.Parser().parse(subject.getSchema());
  }

  @SneakyThrows
  public byte[] getAvroBytes(long id, String content) {
    String hex = "00" + String.format("%08d", id) + content;
    return Hex.decodeHex(hex);
  }
}
