package io.spoud.agoora.agents.pgsql.config;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

public class PostgresqlResource implements QuarkusTestResourceLifecycleManager {

  private PostgreSQLContainer postgres =
      new PostgreSQLContainer<>("postgres:11-alpine")
          .withDatabaseName("postgres")
          .withUsername("postgres")
          .withPassword("postgres");

  @Override
  public Map<String, String> start() {
    postgres.start();
    String url = postgres.getJdbcUrl();
    url = url.substring(0, url.indexOf('?'));
    return Map.of("quarkus.datasource.jdbc.url", url, "quarkus.flyway.migrate-at-start", "true");
  }

  @Override
  public void stop() {
    postgres.stop();
  }
}
