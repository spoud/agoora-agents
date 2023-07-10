package io.spoud.agoora.agents.pgsql.database;

import io.quarkus.test.junit.QuarkusTest;
import io.spoud.agoora.agents.pgsql.AbstractDatabaseTest;
import io.spoud.agoora.agents.pgsql.data.DatabaseDescription;
import io.spoud.agoora.agents.pgsql.data.FieldDescription;
import io.spoud.agoora.agents.pgsql.data.TableDescription;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class DatabaseScrapperTest extends AbstractDatabaseTest {

  @Inject private DatabaseScrapper databaseScrapper;

  @Test
  void testDatabaseDescription(){
    final DatabaseDescription databaseDescription = databaseScrapper.getDatabaseDescription();
    assertThat(databaseDescription.getName()).isEqualTo("postgres");
  }

  @Test
  void testListTables() {
    List<TableDescription> tableDescriptions = databaseScrapper.listTables();
    assertThat(tableDescriptions)
        .extracting(TableDescription::getName)
        .containsExactlyInAnyOrder("t_city", "t_address", "flyway_schema_history");

    TableDescription city =
        tableDescriptions.stream().filter(t -> t.getName().equals("t_city")).findAny().get();
    assertThat(city.getFields())
        .extracting(FieldDescription::getName)
        .containsExactly(
            "city_uuid", "label", "meta", "created", "updated", "created_by", "updated_by");

    FieldDescription city_uuid = getField(city, "city_uuid");
    assertThat(city_uuid.getType()).isEqualTo("uuid");
    assertThat(city_uuid.isNullable()).isFalse();

    FieldDescription label = getField(city, "label");
    assertThat(label.getType()).isEqualTo("varchar");
    assertThat(label.isNullable()).isFalse();

    FieldDescription meta = getField(city, "meta");
    assertThat(meta.getType()).isEqualTo("text");
    assertThat(meta.isNullable()).isTrue();

    FieldDescription created = getField(city, "created");
    assertThat(created.getType()).isEqualTo("timestamp");
    assertThat(created.isNullable()).isFalse();

    FieldDescription updated = getField(city, "updated");
    assertThat(updated.getType()).isEqualTo("timestamp");
    assertThat(updated.isNullable()).isFalse();

    FieldDescription created_by = getField(city, "created_by");
    assertThat(created_by.getType()).isEqualTo("varchar");
    assertThat(created_by.isNullable()).isFalse();

    FieldDescription updated_by = getField(city, "updated_by");
    assertThat(updated_by.getType()).isEqualTo("varchar");
    assertThat(updated_by.isNullable()).isFalse();
  }

  @Test
  void testMetrics() throws Exception {
    assertThat(databaseScrapper.getRowCount("t_city")).isPresent().contains(3L);
    assertThat(databaseScrapper.getRowCount("t_address")).isPresent().contains(2L);
    assertThat(databaseScrapper.getTableSizeBytes("t_city")).isPresent().contains(65536L);
    assertThat(databaseScrapper.getTableSizeBytes("t_address")).isPresent().contains(81920L);
    // metrics seems not to be consistent (certainly aggregation delay or something), so we don't test them
  }

  @Test
  void testSamples() throws Exception {
    assertThat(databaseScrapper.getSamples("t_city", 1).get()).hasSize(1);
    assertThat(databaseScrapper.getSamples("t_city", 2).get()).hasSize(2);
    List<Map<String, Object>> samples = databaseScrapper.getSamples("t_city", 1000).get();
    assertThat(samples).hasSize(3);

    Map<String, Object> sample1 = samples.get(0);
    assertThat(sample1).hasSize(7);

    assertThat(sample1)
        .containsAllEntriesOf(
            Map.of(
                "city_uuid", UUID.fromString("7bb2fdb0-8f05-44e8-b062-8a7d94d83b47"),
                "label", "Bern",
                "meta", "{\"strange_language\":true}",
                "created_by", "script",
                "updated_by", "script"));
  }

  private FieldDescription getField(TableDescription table, String fieldName) {
    return table.getFields().stream()
        .filter(f -> f.getName().equalsIgnoreCase(fieldName))
        .findAny()
        .get();
  }
}
