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
package com.dbui.store.cli;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Command-line ingester for the H2O Store catalog. Reads a JSON Lines file (one product per line)
 * and writes every product into Cassandra (the {@code products} and {@code products_by_category}
 * tables, the system of record) and into the OpenSearch {@code h2o_products} index (used by the
 * site search). The Cassandra schema and the OpenSearch index are created if missing, so the tool
 * can be run standalone before the web application.
 *
 * <p>
 * Usage:
 *
 * <pre>
 *   java -jar store-cli.jar --file products.jsonl \
 *        [--cassandra-host 127.0.0.1] [--cassandra-port 9042] [--datacenter datacenter1] \
 *        [--keyspace h2o_store] [--opensearch-url http://127.0.0.1:9200] \
 *        [--index h2o_products] [--recreate]
 * </pre>
 */
public final class ProductIngestCli {

    private final ObjectMapper mapper = new ObjectMapper();

    private ProductIngestCli() {
    }

    public static void main(String[] args) {
        CliOptions options;
        try {
            options = CliOptions.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            CliOptions.printUsage(System.err);
            System.exit(2);
            return;
        }
        if (options.help()) {
            CliOptions.printUsage(System.out);
            return;
        }
        try {
            int count = new ProductIngestCli().run(options);
            System.out.printf("%nDone. Ingested %d product(s).%n", count);
        } catch (Exception e) {
            System.err.println("Ingest failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private int run(CliOptions options) throws Exception {
        List<JsonNode> products = readProducts(options.file());
        if (products.isEmpty()) {
            System.out.println("No products found in " + options.file());
            return 0;
        }
        System.out.printf("Read %d product(s) from %s%n", products.size(), options.file());

        try (CqlSession session = CqlSession.builder()
                .addContactPoint(
                        new InetSocketAddress(options.cassandraHost(), options.cassandraPort()))
                .withLocalDatacenter(options.datacenter()).build()) {

            CassandraCatalog catalog = new CassandraCatalog(session, options.keyspace());
            catalog.ensureSchema();
            OpenSearchCatalog search = new OpenSearchCatalog(options.openSearchUrl(),
                    options.index(), mapper);

            if (options.recreate()) {
                // Truncate the tables and drop the index so the mapping is rebuilt cleanly.
                System.out.println("Recreating catalog (truncating tables and dropping index)…");
                catalog.truncate();
                search.deleteIndex();
            }
            search.ensureIndex();

            PreparedStatement insertProduct = catalog.prepareInsertProduct();
            PreparedStatement insertByCategory = catalog.prepareInsertByCategory();

            int count = 0;
            for (JsonNode node : products) {
                ParsedProduct p = ParsedProduct.from(node);
                catalog.insert(insertProduct, insertByCategory, p);
                search.index(p);
                count++;
                System.out.printf("  + %-34s %s%n", truncate(p.name(), 34), p.category());
            }
            search.refresh();
            return count;
        }
    }

    private List<JsonNode> readProducts(Path file) throws Exception {
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("File not found: " + file);
        }
        List<JsonNode> nodes = new ArrayList<>();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        int lineNo = 0;
        for (String line : lines) {
            lineNo++;
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            try {
                nodes.add(mapper.readTree(trimmed));
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Invalid JSON on line " + lineNo + ": " + e.getMessage());
            }
        }
        return nodes;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /**
     * A product parsed from one JSON line. A missing {@code id} is generated; {@code stock}
     * defaults to a stocked value.
     */
    record ParsedProduct(UUID id, String name, String description, String category,
            Set<String> tags, BigDecimal price, String image, String species, int stock) {

        static ParsedProduct from(JsonNode node) {
            String name = required(node, "name");
            String category = required(node, "category");
            UUID id = node.hasNonNull("id")
                    ? UUID.fromString(node.get("id").asText())
                    : UUID.randomUUID();
            Set<String> tags = new HashSet<>();
            if (node.has("tags") && node.get("tags").isArray()) {
                node.get("tags").forEach(t -> tags.add(t.asText()));
            }
            BigDecimal price = node.hasNonNull("price")
                    ? new BigDecimal(node.get("price").asText())
                    : BigDecimal.ZERO;
            int stock = node.hasNonNull("stock") ? node.get("stock").asInt() : 25;
            return new ParsedProduct(id, name, text(node, "description"), category, tags, price,
                    text(node, "image"), text(node, "species"), stock);
        }

        private static String required(JsonNode node, String field) {
            if (!node.hasNonNull(field) || node.get(field).asText().isBlank()) {
                throw new IllegalArgumentException("Missing required field '" + field + "'");
            }
            return node.get(field).asText();
        }

        private static String text(JsonNode node, String field) {
            return node.hasNonNull(field) ? node.get(field).asText() : null;
        }
    }
}
