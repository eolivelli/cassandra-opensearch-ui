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
package com.dbui.store.order;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.data.UdtValue;
import com.datastax.oss.driver.api.core.type.UserDefinedType;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.dbui.store.cart.Cart;
import com.dbui.store.cart.CartItem;
import com.dbui.store.cart.CartService;
import com.dbui.store.cassandra.CassandraSessionProvider;
import com.dbui.store.config.CassandraProperties;
import com.dbui.store.customer.Customer;
import com.dbui.store.customer.CustomerService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Places and reads orders. Checkout snapshots the current cart into an immutable order, "charges"
 * the (mock) card, and empties the cart. Everything is stored in Cassandra; the order line items
 * use the {@code order_item} user-defined type.
 */
@Service
public class OrderService {

    private static final Pattern NON_DIGITS = Pattern.compile("\\D");

    private final CassandraSessionProvider sessions;
    private final CartService carts;
    private final CustomerService customers;
    private final String keyspace;
    // Prepared statements are cached and reused rather than re-prepared on every request.
    private final Map<String, PreparedStatement> statements = new ConcurrentHashMap<>();

    public OrderService(CassandraSessionProvider sessions, CartService carts,
            CustomerService customers, CassandraProperties properties) {
        this.sessions = sessions;
        this.carts = carts;
        this.customers = customers;
        this.keyspace = properties.getKeyspace();
    }

    private PreparedStatement prepared(String cql) {
        return statements.computeIfAbsent(cql, c -> sessions.session().prepare(c));
    }

    /**
     * Turns the customer's current cart into a paid order, then clears the cart.
     *
     * @throws IllegalArgumentException
     *             if the cart is empty
     */
    public Order checkout(String username, CheckoutRequest request) {
        Cart cart = carts.get(username);
        if (cart.items().isEmpty()) {
            throw new IllegalArgumentException("Your cart is empty");
        }

        CqlSession session = sessions.session();
        UserDefinedType itemType = session.getMetadata().getKeyspace(keyspace)
                .flatMap(ks -> ks.getUserDefinedType("order_item"))
                .orElseThrow(() -> new IllegalStateException("order_item type is not defined"));

        List<OrderItem> items = new ArrayList<>();
        List<UdtValue> udtItems = new ArrayList<>();
        for (CartItem line : cart.items()) {
            items.add(new OrderItem(line.productId(), line.name(), line.price(), line.image(),
                    line.qty()));
            udtItems.add(itemType.newValue().setUuid("product_id", line.productId())
                    .setString("name", line.name()).setBigDecimal("price", line.price())
                    .setString("image", line.image()).setInt("qty", line.qty()));
        }

        UUID orderId = Uuids.timeBased();
        Instant now = Instant.now();
        String cardLast4 = lastFour(request.cardNumber());

        var stmt = prepared("INSERT INTO " + keyspace + ".orders"
                + " (username, order_id, total, status, created_at, ship_to, card_last4, items)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        session.execute(stmt.bind(username, orderId, cart.subtotal(), "PAID", now, request.shipTo(),
                cardLast4, udtItems));

        // Remember the shipping address as the customer's default for next time.
        customers.find(username)
                .ifPresent(existing -> customers.upsert(new Customer(existing.username(),
                        existing.fullName(), existing.email(), request.shipTo())));

        carts.clear(username);
        return new Order(orderId, username, cart.subtotal(), "PAID", now, request.shipTo(),
                cardLast4, items);
    }

    /** Lists a customer's orders, newest first (single-partition read). */
    public List<Order> list(String username) {
        CqlSession session = sessions.session();
        var stmt = prepared("SELECT * FROM " + keyspace + ".orders WHERE username = ?");
        var rs = session.execute(stmt.bind(username));
        List<Order> orders = new ArrayList<>();
        for (Row row : rs) {
            orders.add(toOrder(row));
        }
        return orders;
    }

    /** Looks up one order by id for the given customer. */
    public Optional<Order> get(String username, UUID orderId) {
        CqlSession session = sessions.session();
        var stmt = prepared(
                "SELECT * FROM " + keyspace + ".orders WHERE username = ? AND order_id = ?");
        Row row = session.execute(stmt.bind(username, orderId)).one();
        return row == null ? Optional.empty() : Optional.of(toOrder(row));
    }

    private Order toOrder(Row row) {
        List<OrderItem> items = new ArrayList<>();
        List<UdtValue> udtItems = row.getList("items", UdtValue.class);
        if (udtItems != null) {
            for (UdtValue udt : udtItems) {
                items.add(new OrderItem(udt.getUuid("product_id"), udt.getString("name"),
                        udt.getBigDecimal("price"), udt.getString("image"), udt.getInt("qty")));
            }
        }
        return new Order(row.getUuid("order_id"), row.getString("username"),
                row.getBigDecimal("total"), row.getString("status"), row.getInstant("created_at"),
                row.getString("ship_to"), row.getString("card_last4"), items);
    }

    private String lastFour(String cardNumber) {
        String digits = NON_DIGITS.matcher(cardNumber).replaceAll("");
        return digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);
    }
}
