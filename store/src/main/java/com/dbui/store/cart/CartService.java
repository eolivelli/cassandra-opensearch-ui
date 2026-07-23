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
package com.dbui.store.cart;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.dbui.store.cassandra.CassandraSessionProvider;
import com.dbui.store.catalog.Product;
import com.dbui.store.catalog.ProductService;
import com.dbui.store.config.CassandraProperties;
import com.dbui.store.web.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Manages the shopping cart, stored in Cassandra in the {@code cart_items} table keyed by username.
 * Each customer's cart is a single partition, so reading and mutating it are single-partition
 * operations.
 */
@Service
public class CartService {

    private final CassandraSessionProvider sessions;
    private final ProductService products;
    private final String keyspace;
    // Prepared statements are cached and reused rather than re-prepared on every request.
    private final Map<String, PreparedStatement> statements = new ConcurrentHashMap<>();

    public CartService(CassandraSessionProvider sessions, ProductService products,
            CassandraProperties properties) {
        this.sessions = sessions;
        this.products = products;
        this.keyspace = properties.getKeyspace();
    }

    private PreparedStatement prepared(String cql) {
        return statements.computeIfAbsent(cql, c -> sessions.session().prepare(c));
    }

    /** Returns the customer's cart with totals. */
    public Cart get(String username) {
        CqlSession session = sessions.session();
        var stmt = prepared("SELECT product_id, name, price, image, qty FROM " + keyspace
                + ".cart_items WHERE username = ?");
        var rs = session.execute(stmt.bind(username));
        List<CartItem> items = new ArrayList<>();
        for (Row row : rs) {
            items.add(new CartItem(row.getUuid("product_id"), row.getString("name"),
                    row.getBigDecimal("price"), row.getString("image"), row.getInt("qty")));
        }
        // Stable ordering for the UI regardless of Cassandra's clustering order.
        items.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return Cart.of(items);
    }

    /**
     * Adds {@code qty} units of a product to the cart, accumulating onto any existing quantity.
     * Product details are copied from the catalog at add-time.
     */
    public Cart addItem(String username, UUID productId, int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        Product product = products.byId(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));
        int existing = currentQty(username, productId);
        int newQty = existing + qty;

        CqlSession session = sessions.session();
        var stmt = prepared("INSERT INTO " + keyspace
                + ".cart_items (username, product_id, name, price, image, qty)"
                + " VALUES (?, ?, ?, ?, ?, ?)");
        session.execute(stmt.bind(username, productId, product.name(), product.price(),
                product.image(), newQty));
        return get(username);
    }

    /** Sets the absolute quantity for a line; a quantity of zero or less removes it. */
    public Cart setQuantity(String username, UUID productId, int qty) {
        if (qty <= 0) {
            return removeItem(username, productId);
        }
        Product product = products.byId(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));
        CqlSession session = sessions.session();
        var stmt = prepared("INSERT INTO " + keyspace
                + ".cart_items (username, product_id, name, price, image, qty)"
                + " VALUES (?, ?, ?, ?, ?, ?)");
        session.execute(stmt.bind(username, productId, product.name(), product.price(),
                product.image(), qty));
        return get(username);
    }

    /** Removes a single line from the cart. */
    public Cart removeItem(String username, UUID productId) {
        CqlSession session = sessions.session();
        var stmt = prepared(
                "DELETE FROM " + keyspace + ".cart_items WHERE username = ? AND product_id = ?");
        session.execute(stmt.bind(username, productId));
        return get(username);
    }

    /** Empties the whole cart (a single-partition delete). */
    public void clear(String username) {
        CqlSession session = sessions.session();
        var stmt = prepared("DELETE FROM " + keyspace + ".cart_items WHERE username = ?");
        session.execute(stmt.bind(username));
    }

    private int currentQty(String username, UUID productId) {
        CqlSession session = sessions.session();
        var stmt = prepared("SELECT qty FROM " + keyspace
                + ".cart_items WHERE username = ? AND product_id = ?");
        Row row = session.execute(stmt.bind(username, productId)).one();
        return row == null ? 0 : row.getInt("qty");
    }
}
