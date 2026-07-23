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
package com.dbui.store.customer;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.dbui.store.cassandra.CassandraSessionProvider;
import com.dbui.store.config.CassandraProperties;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/** Stores and retrieves customer profiles from Cassandra ({@code customers} table). */
@Service
public class CustomerService {

    private final CassandraSessionProvider sessions;
    private final String keyspace;
    // Prepared statements are cached and reused rather than re-prepared on every request.
    private final Map<String, PreparedStatement> statements = new ConcurrentHashMap<>();

    public CustomerService(CassandraSessionProvider sessions, CassandraProperties properties) {
        this.sessions = sessions;
        this.keyspace = properties.getKeyspace();
    }

    private PreparedStatement prepared(String cql) {
        return statements.computeIfAbsent(cql, c -> sessions.session().prepare(c));
    }

    /** Returns the customer profile if present. */
    public Optional<Customer> find(String username) {
        CqlSession session = sessions.session();
        var stmt = prepared("SELECT username, full_name, email, address FROM " + keyspace
                + ".customers WHERE username = ?");
        Row row = session.execute(stmt.bind(username)).one();
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(new Customer(row.getString("username"), row.getString("full_name"),
                row.getString("email"), row.getString("address")));
    }

    /** Creates the profile if it does not exist yet; leaves an existing profile untouched. */
    public Customer ensureExists(String username, String fullName) {
        Optional<Customer> existing = find(username);
        if (existing.isPresent()) {
            return existing.get();
        }
        Customer customer = new Customer(username, fullName, username + "@h2o.example", "");
        upsert(customer);
        return customer;
    }

    /** Inserts or updates a customer profile. */
    public void upsert(Customer customer) {
        CqlSession session = sessions.session();
        var stmt = prepared("INSERT INTO " + keyspace
                + ".customers (username, full_name, email, address) VALUES (?, ?, ?, ?)");
        session.execute(stmt.bind(customer.username(), customer.fullName(), customer.email(),
                customer.address()));
    }
}
