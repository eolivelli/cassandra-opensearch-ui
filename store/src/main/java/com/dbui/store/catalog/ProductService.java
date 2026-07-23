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
package com.dbui.store.catalog;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.dbui.store.cassandra.CassandraSessionProvider;
import com.dbui.store.config.CassandraProperties;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Reads the product catalog from Cassandra, the system of record. Two tables back the two access
 * patterns: {@code products} for point look-ups by id, and {@code products_by_category} for
 * browsing a category as a single-partition read. OpenSearch is deliberately not used here - it
 * powers only the free-text search feature (see the search package).
 */
@Service
public class ProductService {

    private final CassandraSessionProvider sessions;
    private final String keyspace;
    // Prepared statements are cached and reused rather than re-prepared on every request.
    private final Map<String, PreparedStatement> statements = new ConcurrentHashMap<>();

    public ProductService(CassandraSessionProvider sessions, CassandraProperties properties) {
        this.sessions = sessions;
        this.keyspace = properties.getKeyspace();
    }

    private PreparedStatement prepared(String cql) {
        return statements.computeIfAbsent(cql, c -> sessions.session().prepare(c));
    }

    /** Returns the distinct category names, sorted alphabetically. */
    public List<String> categories() {
        CqlSession session = sessions.session();
        // category is the full partition key, so DISTINCT is an efficient metadata-style read.
        var rs = session
                .execute("SELECT DISTINCT category FROM " + keyspace + ".products_by_category");
        TreeSet<String> categories = new TreeSet<>();
        for (Row row : rs) {
            categories.add(row.getString("category"));
        }
        return new ArrayList<>(categories);
    }

    /** Returns all products in a category, ordered by name (single-partition read). */
    public List<Product> byCategory(String category) {
        CqlSession session = sessions.session();
        var stmt = prepared(
                "SELECT * FROM " + keyspace + ".products_by_category WHERE category = ?");
        var rs = session.execute(stmt.bind(category));
        List<Product> products = new ArrayList<>();
        for (Row row : rs) {
            products.add(toProduct(row));
        }
        return products;
    }

    /** Looks up a single product by id (point read); empty if it does not exist. */
    public Optional<Product> byId(UUID id) {
        CqlSession session = sessions.session();
        var stmt = prepared("SELECT * FROM " + keyspace + ".products WHERE id = ?");
        Row row = session.execute(stmt.bind(id)).one();
        return row == null ? Optional.empty() : Optional.of(toProduct(row));
    }

    /**
     * Returns a handful of "featured" products for the home page by taking the first product from
     * each category. Every read is a single-partition read, so this stays efficient as the catalog
     * grows.
     */
    public List<Product> featured(int limit) {
        List<Product> featured = new ArrayList<>();
        for (String category : categories()) {
            List<Product> inCategory = byCategory(category);
            if (!inCategory.isEmpty()) {
                featured.add(inCategory.get(0));
            }
            if (featured.size() >= limit) {
                break;
            }
        }
        return featured;
    }

    private Product toProduct(Row row) {
        // tags is a Cassandra set<text>; expose it as a sorted list for a stable JSON shape.
        List<String> tags = new ArrayList<>(new TreeSet<>(row.getSet("tags", String.class)));
        BigDecimal price = row.getBigDecimal("price");
        return new Product(row.getUuid("id"), row.getString("name"), row.getString("description"),
                row.getString("category"), tags, price, row.getString("image"),
                row.getString("species"),
                row.getColumnDefinitions().contains("stock") ? row.getInt("stock") : 0);
    }
}
