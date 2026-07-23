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
package com.dbui.store;

import static org.assertj.core.api.Assertions.assertThat;

import com.datastax.oss.driver.api.core.CqlSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Full-stack integration test: boots the whole store application and drives it through its HTTP API
 * against a real Cassandra and OpenSearch (see {@link AbstractIntegrationTest}). Uses a
 * cookie-aware {@link HttpClient} so the session-based mock login is exercised end to end: sign in,
 * add to cart, check out, and read the resulting order.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StoreIntegrationTest extends AbstractIntegrationTest {

    private static final String KEYSPACE = "h2o_store";
    private static final String TETRA_ID = "11111111-1111-1111-1111-111111111111";

    @DynamicPropertySource
    static void wireDatabases(DynamicPropertyRegistry registry) {
        registry.add("store.cassandra.host", () -> CASSANDRA.getContactPoint().getHostString());
        registry.add("store.cassandra.port", () -> CASSANDRA.getContactPoint().getPort());
        registry.add("store.cassandra.local-datacenter", CASSANDRA::getLocalDatacenter);
        registry.add("store.opensearch.url", OPENSEARCH::getHttpHostAddress);
    }

    @LocalServerPort
    private int port;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager())
            .build();

    @BeforeAll
    static void seed() throws Exception {
        try (CqlSession session = CqlSession.builder().addContactPoint(CASSANDRA.getContactPoint())
                .withLocalDatacenter(CASSANDRA.getLocalDatacenter()).build()) {
            session.execute("CREATE KEYSPACE IF NOT EXISTS " + KEYSPACE
                    + " WITH replication = {'class':'SimpleStrategy','replication_factor':1}");
            session.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE + ".products ("
                    + "id uuid PRIMARY KEY, name text, description text, category text,"
                    + " tags set<text>, price decimal, image text, species text, stock int,"
                    + " created_at timestamp)");
            session.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE + ".products_by_category ("
                    + "category text, name text, id uuid, description text, tags set<text>,"
                    + " price decimal, image text, species text, stock int,"
                    + " PRIMARY KEY (category, name, id))");
            insertProduct(session, "products", true);
            insertProduct(session, "products_by_category", false);
        }
        indexProduct();
    }

    private static void insertProduct(CqlSession session, String table, boolean byId) {
        String columns = byId
                ? "id, name, description, category, tags, price, image, species, stock, created_at"
                : "category, name, id, description, tags, price, image, species, stock";
        String values = byId
                ? TETRA_ID + ", 'Neon Tetra', 'A dazzling nano schooler', 'Live Fish',"
                        + " {'schooling','freshwater'}, 4.99, '/products/neon-tetra.jpg',"
                        + " 'Paracheirodon innesi', 120, toTimestamp(now())"
                : "'Live Fish', 'Neon Tetra', " + TETRA_ID + ", 'A dazzling nano schooler',"
                        + " {'schooling','freshwater'}, 4.99, '/products/neon-tetra.jpg',"
                        + " 'Paracheirodon innesi', 120";
        session.execute("INSERT INTO " + KEYSPACE + "." + table + " (" + columns + ") VALUES ("
                + values + ")");
    }

    private static void indexProduct() throws Exception {
        String doc = "{\"name\":\"Neon Tetra\",\"description\":\"A dazzling nano schooler\","
                + "\"category\":\"Live Fish\",\"tags\":[\"schooling\",\"freshwater\"],"
                + "\"price\":4.99,\"image\":\"/products/neon-tetra.jpg\","
                + "\"species\":\"Paracheirodon innesi\",\"stock\":120}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENSEARCH.getHttpHostAddress() + "/h2o_products/_doc/" + TETRA_ID
                        + "?refresh=true"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(doc, StandardCharsets.UTF_8)).build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isLessThan(300);
    }

    @Test
    @Order(1)
    void catalogIsServedFromCassandra() throws Exception {
        JsonNode categories = get("/api/categories");
        assertThat(values(categories)).contains("Live Fish");

        JsonNode products = get("/api/products?category=Live%20Fish");
        assertThat(products).isNotEmpty();
        assertThat(products.get(0).path("name").asText()).isEqualTo("Neon Tetra");
        assertThat(products.get(0).path("species").asText()).isEqualTo("Paracheirodon innesi");

        JsonNode single = get("/api/products/" + TETRA_ID);
        assertThat(single.path("name").asText()).isEqualTo("Neon Tetra");
        assertThat(single.path("stock").asInt()).isEqualTo(120);
    }

    @Test
    @Order(2)
    void searchIsPoweredByOpenSearch() throws Exception {
        JsonNode result = get("/api/search?q=tetra");
        assertThat(result.path("total").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(result.has("tookMillis")).isTrue();
        assertThat(result.path("hits").get(0).path("name").asText()).isEqualTo("Neon Tetra");
    }

    @Test
    @Order(3)
    void cartRequiresAuthentication() throws Exception {
        HttpResponse<String> response = send("GET", "/api/cart", null);
        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    @Order(4)
    void shoppingFlowLoginAddToCartCheckout() throws Exception {
        JsonNode me = postJson("/api/auth/login",
                "{\"username\":\"user\",\"password\":\"password\"}");
        assertThat(me.path("username").asText()).isEqualTo("user");

        JsonNode cart = postJson("/api/cart/items",
                "{\"productId\":\"" + TETRA_ID + "\",\"qty\":2}");
        assertThat(cart.path("count").asInt()).isEqualTo(2);
        assertThat(cart.path("subtotal").asDouble()).isEqualTo(9.98);

        JsonNode order = postJson("/api/checkout",
                "{\"shipTo\":\"1 Coral Way\",\"cardName\":\"Demo Customer\","
                        + "\"cardNumber\":\"4242 4242 4242 4242\",\"expiry\":\"12/29\","
                        + "\"cvv\":\"123\"}");
        assertThat(order.path("status").asText()).isEqualTo("PAID");
        assertThat(order.path("cardLast4").asText()).isEqualTo("4242");
        assertThat(order.path("total").asDouble()).isEqualTo(9.98);
        assertThat(order.path("items").get(0).path("name").asText()).isEqualTo("Neon Tetra");

        // Cart is emptied after checkout.
        JsonNode emptied = get("/api/cart");
        assertThat(emptied.path("count").asInt()).isZero();

        // The order is now listed in history.
        JsonNode orders = get("/api/orders");
        assertThat(orders).isNotEmpty();
        assertThat(orders.get(0).path("status").asText()).isEqualTo("PAID");
    }

    private JsonNode get(String path) throws Exception {
        HttpResponse<String> response = send("GET", path, null);
        assertThat(response.statusCode()).as("GET %s -> %s", path, response.body()).isLessThan(300);
        return mapper.readTree(response.body());
    }

    private JsonNode postJson(String path, String body) throws Exception {
        HttpResponse<String> response = send("POST", path, body);
        assertThat(response.statusCode()).as("POST %s -> %s", path, response.body())
                .isLessThan(300);
        return mapper.readTree(response.body());
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path)).method(method, publisher);
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static java.util.List<String> values(JsonNode array) {
        java.util.List<String> values = new java.util.ArrayList<>();
        array.forEach(node -> values.add(node.asText()));
        return values;
    }
}
