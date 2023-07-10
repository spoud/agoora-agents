package io.spoud.agoora.agents.pgsql.database.postgres;

import io.spoud.agoora.agents.pgsql.data.DatabaseDescription;
import io.spoud.agoora.agents.pgsql.data.FieldDescription;
import io.spoud.agoora.agents.pgsql.data.TableDescription;
import io.spoud.agoora.agents.pgsql.database.DatabaseScrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.jdbc.PgResultSetMetaData;

import jakarta.enterprise.context.ApplicationScoped;
import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class PostgresScrapper implements DatabaseScrapper {

  private static final Pattern TABLE_NAME_PATTER = Pattern.compile("[a-zA-Z0-9_]*");
  private final Connection connection;

  @Override
  public DatabaseDescription getDatabaseDescription() {
    final DatabaseDescription.DatabaseDescriptionBuilder builder =
        DatabaseDescription.builder().tables(listTables());
    try {
      final DatabaseMetaData metaData = connection.getMetaData();

      String url = metaData.getURL();
      String databaseName = StringUtils.substringAfterLast(url, "/");

      builder.name(databaseName);
    } catch (Exception ex) {
      LOG.error("Unable to gather database metadata", ex);
    }

    return builder.build();
  }

  @Override
  public List<TableDescription> listTables() {
    List<TableDescription> list = new ArrayList<>();
    try {
      // Retrieving the meta data object
      DatabaseMetaData metaData = connection.getMetaData();
      String[] types = {"TABLE"};
      // Retrieving the columns in the database
      ResultSet tables = metaData.getTables(null, null, "%", types);
      while (tables.next()) {
        String tableName = tables.getString("TABLE_NAME");
        list.add(describeTable(tableName));
      }
    } catch (Exception ex) {
      LOG.error("Error while fetching tables", ex);
    }
    return list;
  }

  private TableDescription describeTable(String tableName) {
    List<FieldDescription> fields = new ArrayList<>();
    try (Statement stmt = connection.createStatement()) {
      ResultSet rs =
          stmt.executeQuery(queryWithTableName("select * from $tableName LIMIT 1", tableName));
      PgResultSetMetaData rsmd = (PgResultSetMetaData) rs.getMetaData();
      for (int i = 1; i <= rsmd.getColumnCount(); i++) {
        fields.add(
            FieldDescription.builder()
                .name(rsmd.getColumnName(i))
                .type(rsmd.getColumnTypeName(i))
                .nullable(rsmd.isNullable(i) == 1)
                .build());
      }
    } catch (Exception ex) {
      LOG.error("Unable to describe table {}", tableName, ex);
    }
    return TableDescription.builder().name(tableName).fields(fields).build();
  }

  @Override
  public Optional<Long> getRowCount(String tableName) {
    return getFirstResult(queryWithTableName("select COUNT(*) from $tableName", tableName));
  }

  @Override
  public Optional<Long> getTableSizeBytes(String tableName) {
    return getFirstResult(
        queryWithTableName("SELECT pg_total_relation_size('$tableName')", tableName));
  }

  @Override
  public Optional<Long> getChangesCount(String tableName) {
    return getFirstResult(
        queryWithTableName(
            "select n_tup_ins+ n_tup_upd+ n_tup_del as changes from pg_stat_user_tables WHERE relname='$tableName'",
            tableName));
  }

  private Optional<Long> getFirstResult(String query) {
    try (Statement stmt = connection.createStatement()) {
      ResultSet rs = stmt.executeQuery(query);
      rs.next();
      return Optional.of(rs.getLong(1));
    } catch (Exception ex) {
      LOG.error("query failed: {}", query, ex);
    }
    return Optional.empty();
  }

  // TODO order by something ?
  @Override
  public Optional<List<Map<String, Object>>> getSamples(String tableName, int size) {
    String query = queryWithTableName("SELECT * FROM $tableName LIMIT ?", tableName);
    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      Instant start = Instant.now();
      stmt.setInt(1, size);
      ResultSet rs = stmt.executeQuery();
      List<Map<String, Object>> rows = new ArrayList<>(size);
      ResultSetMetaData rsmd = rs.getMetaData();
      LOG.debug(
          "Query '{}' on table {} with limit {} took {}",
          query,
          tableName,
          size,
          Duration.between(start, Instant.now()));

      while (rs.next()) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
          String columnName = rsmd.getColumnName(i);
          Object value = rs.getObject(i);
          row.put(columnName, value);
        }
        rows.add(row);
      }
      return Optional.of(rows);
    } catch (Exception ex) {
      LOG.error("Failed to get samples for table {}", tableName, ex);
    }
    return Optional.empty();
  }

  // FIXME I don't think this is good enough to avoid SQL injections !
  private void checkTableName(String table) {
    if (!TABLE_NAME_PATTER.matcher(table).find()) {
      throw new InvalidParameterException("Table parameter is invalid : '" + table + "'");
    }
  }

  /** Replace $tableName with an encoded tableName (to avoid sql injections) */
  private String queryWithTableName(String query, String tableName) {
    checkTableName(tableName);
    return query.replace("$tableName", tableName);
  }
}
