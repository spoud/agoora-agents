package io.spoud.agoora.agents.pgsql.database;

import io.spoud.agoora.agents.pgsql.data.DatabaseDescription;
import io.spoud.agoora.agents.pgsql.data.TableDescription;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Database abstraction */
public interface DatabaseScrapper {

  /**
   * Get database description
   */
  DatabaseDescription getDatabaseDescription();

  /**
   * List the available tables
   */
  List<TableDescription> listTables();

  /**
   * Row count for a table
   */
  Optional<Long> getRowCount(String table);

  /**
   * Size (in bytes) of a table
   */
  Optional<Long> getTableSizeBytes(String table);

  /**
   * Get the number of changes that happen to a table (insert/update/delete)
   */
  Optional<Long> getChangesCount(String table);

  /**
   * Get a few samples. Best would be to have the latest rows
   */
  Optional<List<Map<String, Object>>> getSamples(String tableName, int size);
}
