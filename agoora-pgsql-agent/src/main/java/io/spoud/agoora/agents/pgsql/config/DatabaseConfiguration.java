package io.spoud.agoora.agents.pgsql.config;

import io.agroal.api.AgroalDataSource;
import io.agroal.pool.DataSource;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
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
