package io.spoud.agoora.agents.kafka.schema.confluent;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.RequestScoped;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@RegisterForReflection
@RegisterRestClient(configKey = "rest-confluent-registry")
@Path("/subjects/{subject}")
@RegisterClientHeaders(ConfluentAuthHeader.class)
// ! Keep @RequestScoped to avoid memory leak, see https://github.com/spoud/agoora-agents/pull/26#
@RequestScoped
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
