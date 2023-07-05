package io.spoud.agoora.agents.pgsql.config;

import io.agroal.api.AgroalDataSource;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Dependent
public class DatabaseConfiguration {

  @Produces
  public Connection postgresConnection(AgroalDataSource defaultDataSource) throws SQLException {
    return defaultDataSource.getConnection();
  }
}
