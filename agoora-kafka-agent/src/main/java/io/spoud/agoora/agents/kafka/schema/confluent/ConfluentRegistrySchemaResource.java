package io.spoud.agoora.agents.kafka.schema.confluent;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@RegisterForReflection
@RegisterRestClient(configKey = "rest-confluent-registry")
@Path("/schemas")
@RegisterClientHeaders(ConfluentAuthHeader.class)
public interface ConfluentRegistrySchemaResource {

  @GET
  @Path("/{id}")
  @Produces("application/vnd.schemaregistry.v1+json")
  SchemaRegistrySubject getById(@PathParam("id") long id);
}
