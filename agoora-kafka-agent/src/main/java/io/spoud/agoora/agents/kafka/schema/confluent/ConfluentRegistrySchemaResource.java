package io.spoud.agoora.agents.kafka.schema.confluent;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.RequestScoped;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@RegisterForReflection
@RegisterRestClient(configKey = "rest-confluent-registry")
@Path("/schemas")
@RegisterClientHeaders(ConfluentAuthHeader.class)
@RequestScoped
public interface ConfluentRegistrySchemaResource {

  @GET
  @Path("/ids/{id}")
  @Produces("application/vnd.schemaregistry.v1+json")
  SchemaRegistrySubject getById(@PathParam("id") long id);
}
