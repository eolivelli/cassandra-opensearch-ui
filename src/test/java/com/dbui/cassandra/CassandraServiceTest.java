package com.dbui.cassandra;

import static org.assertj.core.api.Assertions.assertThat;

import com.datastax.oss.driver.api.core.CqlSession;
import com.dbui.config.CassandraProperties;
import com.dbui.model.CassandraSchema;
import com.dbui.model.CqlResult;
import com.dbui.model.TableRows;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for {@link CassandraService} against a real Cassandra 5
 * container.
 */
@Testcontainers
class CassandraServiceTest {

    @Container
    static final CassandraContainer CASSANDRA = new CassandraContainer("cassandra:5.0");

    static CassandraSessionProvider provider;
    static CassandraService service;

    @BeforeAll
    static void setUp() {
        InetSocketAddress contactPoint = CASSANDRA.getContactPoint();
        String datacenter = CASSANDRA.getLocalDatacenter();

        // Seed schema and data.
        try (CqlSession session = CqlSession.builder()
                .addContactPoint(contactPoint)
                .withLocalDatacenter(datacenter)
                .build()) {
            session.execute("CREATE KEYSPACE IF NOT EXISTS shop "
                    + "WITH replication = {'class':'SimpleStrategy','replication_factor':1}");
            session.execute("CREATE TABLE shop.products "
                    + "(id int PRIMARY KEY, name text, price double, in_stock boolean)");
            session.execute("INSERT INTO shop.products (id, name, price, in_stock) "
                    + "VALUES (1, 'Keyboard', 49.9, true)");
            session.execute("INSERT INTO shop.products (id, name, price, in_stock) "
                    + "VALUES (2, 'Mouse', 19.5, false)");
        }

        CassandraProperties props = new CassandraProperties();
        props.setHost(contactPoint.getHostString());
        props.setPort(contactPoint.getPort());
        props.setLocalDatacenter(datacenter);

        provider = new CassandraSessionProvider(props);
        service = new CassandraService(provider);
    }

    @AfterAll
    static void tearDown() {
        if (provider != null) {
            provider.close();
        }
    }

    @Test
    void listsKeyspacesIncludingUserKeyspace() {
        CqlResult result = service.listKeyspaces();

        assertThat(result.cql()).contains("system_schema.keyspaces");
        List<String> names = result.rows().stream()
                .map(r -> (String) r.get("keyspace_name"))
                .toList();
        assertThat(names).contains("shop", "system");
    }

    @Test
    void listsTablesOfKeyspace() {
        CqlResult result = service.listTables("shop");

        assertThat(result.cql()).contains("system_schema.tables");
        List<String> tables = result.rows().stream()
                .map(r -> (String) r.get("table_name"))
                .toList();
        assertThat(tables).containsExactly("products");
    }

    @Test
    void returnsTableRowsWithColumnsAndCql() {
        TableRows result = service.tableRows("shop", "products", 100);

        assertThat(result.cql()).isEqualTo("SELECT * FROM \"shop\".\"products\" LIMIT 100");
        assertThat(result.columns()).contains("id", "name", "price", "in_stock");
        assertThat(result.primaryKey()).containsExactly("id");
        assertThat(result.rowCount()).isEqualTo(2);

        Map<String, Object> keyboard = result.rows().stream()
                .filter(r -> Integer.valueOf(1).equals(r.get("id")))
                .findFirst()
                .orElseThrow();
        assertThat(keyboard.get("name")).isEqualTo("Keyboard");
        assertThat(keyboard.get("in_stock")).isEqualTo(true);
    }

    @Test
    void honoursRowLimit() {
        TableRows result = service.tableRows("shop", "products", 1);

        assertThat(result.cql()).endsWith("LIMIT 1");
        assertThat(result.rowCount()).isEqualTo(1);
    }

    @Test
    void describesSchemaWithIndexesAndUdts() {
        // Add a UDT, an extra table using it, and a SAI index to exercise the schema view.
        try (CqlSession session = CqlSession.builder()
                .addContactPoint(CASSANDRA.getContactPoint())
                .withLocalDatacenter(CASSANDRA.getLocalDatacenter())
                .build()) {
            session.execute("CREATE TYPE IF NOT EXISTS shop.address "
                    + "(street text, city text, country text)");
            session.execute("CREATE TABLE IF NOT EXISTS shop.customers "
                    + "(id int PRIMARY KEY, name text, billing_address frozen<address>)");
            session.execute("CREATE CUSTOM INDEX IF NOT EXISTS customers_name_sai "
                    + "ON shop.customers(name) USING 'StorageAttachedIndex'");
        }

        CassandraSchema schema = service.tableSchema("shop", "customers");

        assertThat(schema.columns())
                .anySatisfy(c -> {
                    assertThat(c.name()).isEqualTo("id");
                    assertThat(c.kind()).isEqualTo("PARTITION_KEY");
                });
        assertThat(schema.indexes())
                .anySatisfy(i -> {
                    assertThat(i.name()).isEqualTo("customers_name_sai");
                    assertThat(i.kind()).isEqualTo("SAI");
                    assertThat(i.target()).isEqualTo("name");
                });
        assertThat(schema.types())
                .anySatisfy(t -> assertThat(t.name()).isEqualTo("address"));
        assertThat(schema.createStatement()).contains("CREATE TABLE shop.customers");
    }
}
