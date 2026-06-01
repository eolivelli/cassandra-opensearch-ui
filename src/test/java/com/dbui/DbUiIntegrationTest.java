/*
 * Copyright 2026 Enrico Olivelli
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dbui;

import static org.assertj.core.api.Assertions.assertThat;

import com.datastax.oss.driver.api.core.CqlSession;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Full-stack integration test: boots the entire Spring Boot application and drives it through its
 * HTTP REST API, against a real Cassandra and a real OpenSearch launched by Testcontainers (see
 * {@link AbstractIntegrationTest}). The cluster connection details are injected into the
 * application via {@link DynamicPropertySource}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DbUiIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void wireDatabases(DynamicPropertyRegistry registry) {
        registry.add("dbui.cassandra.host", () -> CASSANDRA.getContactPoint().getHostString());
        registry.add("dbui.cassandra.port", () -> CASSANDRA.getContactPoint().getPort());
        registry.add("dbui.cassandra.local-datacenter", CASSANDRA::getLocalDatacenter);
        registry.add("dbui.opensearch.url", OPENSEARCH::getHttpHostAddress);
    }

    @Autowired
    private TestRestTemplate rest;

    @BeforeAll
    static void seed() throws Exception {
        try (CqlSession session = CqlSession.builder().addContactPoint(CASSANDRA.getContactPoint())
                .withLocalDatacenter(CASSANDRA.getLocalDatacenter()).build()) {
            session.execute("CREATE KEYSPACE IF NOT EXISTS it_store "
                    + "WITH replication = {'class':'SimpleStrategy','replication_factor':1}");
            session.execute("CREATE TABLE it_store.products "
                    + "(id int PRIMARY KEY, name text, price double, in_stock boolean)");
            session.execute("INSERT INTO it_store.products (id, name, price, in_stock) "
                    + "VALUES (1, 'Keyboard', 49.9, true)");
            session.execute("INSERT INTO it_store.products (id, name, price, in_stock) "
                    + "VALUES (2, 'Mouse', 19.5, false)");
        }

        indexDocument("1", "{\"name\":\"Keyboard\",\"price\":49.9,\"in_stock\":true}");
        indexDocument("2", "{\"name\":\"Mouse\",\"price\":19.5,\"in_stock\":false}");
    }

    private static void indexDocument(String id, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENSEARCH.getHttpHostAddress() + "/it_products/_doc/" + id
                        + "?refresh=true"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isLessThan(300);
    }

    @Test
    void cassandraKeyspacesEndpointListsSeededKeyspace() {
        JsonNode body = rest.getForObject("/api/cassandra/keyspaces", JsonNode.class);
        assertThat(body.path("cql").asText()).contains("system_schema.keyspaces");
        assertThat(jsonValues(body.path("rows"), "keyspace_name")).contains("it_store");
    }

    @Test
    void cassandraRowsEndpointReturnsRowsAndCql() {
        JsonNode body = rest.getForObject(
                "/api/cassandra/keyspaces/it_store/tables/products/rows?limit=100", JsonNode.class);
        assertThat(body.path("cql").asText())
                .isEqualTo("SELECT * FROM \"it_store\".\"products\" LIMIT 100");
        assertThat(body.path("primaryKey").get(0).asText()).isEqualTo("id");
        assertThat(body.path("rowCount").asInt()).isEqualTo(2);
        assertThat(jsonValues(body.path("rows"), "name")).contains("Keyboard", "Mouse");
    }

    @Test
    void cassandraSchemaEndpointDescribesTable() {
        JsonNode body = rest.getForObject(
                "/api/cassandra/keyspaces/it_store/tables/products/schema", JsonNode.class);
        assertThat(body.path("createStatement").asText())
                .contains("CREATE TABLE it_store.products");
        assertThat(jsonValues(body.path("columns"), "name")).contains("id", "name", "price",
                "in_stock");
    }

    @Test
    void openSearchIndicesEndpointListsSeededIndex() {
        JsonNode body = rest.getForObject("/api/opensearch/indices", JsonNode.class);
        assertThat(body.path("request").path("path").asText()).contains("/_cat/indices");
        assertThat(jsonValues(body.path("indices"), "index")).contains("it_products");
    }

    @Test
    void openSearchDocumentsEndpointReturnsDocumentsAndRequest() {
        JsonNode body = rest.getForObject("/api/opensearch/indices/it_products/documents?size=50",
                JsonNode.class);
        assertThat(body.path("request").path("method").asText()).isEqualTo("POST");
        assertThat(body.path("request").path("path").asText()).isEqualTo("/it_products/_search");
        assertThat(body.path("total").asLong()).isEqualTo(2);
        assertThat(jsonValues(body.path("documents"), "name")).contains("Keyboard", "Mouse");
    }

    @Test
    void openSearchSchemaEndpointReturnsMapping() {
        JsonNode body = rest.getForObject("/api/opensearch/indices/it_products/schema",
                JsonNode.class);
        assertThat(body.path("request").path("path").asText()).isEqualTo("/it_products");
        assertThat(body.path("mappings").path("properties").has("name")).isTrue();
    }

    /** Collects the values of {@code field} across an array of JSON objects. */
    private static java.util.List<String> jsonValues(JsonNode array, String field) {
        java.util.List<String> values = new java.util.ArrayList<>();
        array.forEach(node -> values.add(node.path(field).asText()));
        return values;
    }
}
