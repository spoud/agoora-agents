package io.spoud.agoora.agents.kafka.schema.confluent;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.spoud.agoora.agents.kafka.schema.KafkaStreamPart;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@RegisterForReflection
@RegisterRestClient(configKey = "rest-confluent-registry")
@Path("/subjects/{subject}")
@RegisterClientHeaders(ConfluentAuthHeader.class)
public interface ConfluentRegistrySubjectResource {

  @GET
  @Path("/versions/latest")
  @Produces("application/vnd.schemaregistry.v1+json")
  SchemaRegistrySubject getLatestSubject(
      @PathParam("subject") String topic);

  @POST
  @Path("versions")
  @Produces({
    "application/vnd.schemaregistry.v1+json",
    "application/vnd.schemaregistry+json",
    "application/json"
  })
  @Consumes("application/json")
  SchemaRegistrySubject addNewSchemaVersion(
          @PathParam("subject") String topic, String content);
}
