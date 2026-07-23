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
package com.dbui.store.cassandra;

import com.dbui.store.config.CassandraProperties;
import com.dbui.store.config.OpenSearchProperties;
import com.dbui.store.opensearch.OpenSearchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Creates the Cassandra schema (keyspace, tables and the {@code order_item} UDT) and the OpenSearch
 * product index on startup. Everything is idempotent ({@code IF NOT EXISTS}). If the databases are
 * not reachable at boot the failure is logged and the application still starts, matching the DB UI
 * module's resilience; the schema can be created later by restarting once the databases are up, or
 * by the ingest CLI.
 */
@Component
@Order(0)
public class SchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

    private final CassandraSessionProvider sessions;
    private final OpenSearchClient openSearch;
    private final String keyspace;
    private final String index;

    public SchemaInitializer(CassandraSessionProvider sessions, OpenSearchClient openSearch,
            CassandraProperties cassandra, OpenSearchProperties opensearch) {
        this.sessions = sessions;
        this.openSearch = openSearch;
        this.keyspace = cassandra.getKeyspace();
        this.index = opensearch.getIndex();
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            createCassandraSchema();
            log.info("Cassandra schema ready in keyspace '{}'", keyspace);
        } catch (RuntimeException e) {
            log.warn("Could not initialize Cassandra schema (is Cassandra running?): {}",
                    e.getMessage());
        }
        try {
            createOpenSearchIndex();
            log.info("OpenSearch index '{}' ready", index);
        } catch (RuntimeException e) {
            log.warn("Could not initialize OpenSearch index (is OpenSearch running?): {}",
                    e.getMessage());
        }
    }

    /** Creates the keyspace, UDT and all tables. Safe to run repeatedly. */
    public void createCassandraSchema() {
        var session = sessions.session();
        session.execute("CREATE KEYSPACE IF NOT EXISTS " + keyspace
                + " WITH replication = {'class':'SimpleStrategy','replication_factor':1}");

        session.execute("CREATE TABLE IF NOT EXISTS " + keyspace + ".products ("
                + "id uuid PRIMARY KEY, name text, description text, category text,"
                + " tags set<text>, price decimal, image text, species text, stock int,"
                + " created_at timestamp)");

        session.execute("CREATE TABLE IF NOT EXISTS " + keyspace + ".products_by_category ("
                + "category text, name text, id uuid, description text, tags set<text>,"
                + " price decimal, image text, species text, stock int,"
                + " PRIMARY KEY (category, name, id))");

        session.execute("CREATE TABLE IF NOT EXISTS " + keyspace + ".customers ("
                + "username text PRIMARY KEY, full_name text, email text, address text)");

        session.execute("CREATE TABLE IF NOT EXISTS " + keyspace + ".cart_items ("
                + "username text, product_id uuid, name text, price decimal, image text, qty int,"
                + " PRIMARY KEY (username, product_id))");

        session.execute("CREATE TYPE IF NOT EXISTS " + keyspace + ".order_item ("
                + "product_id uuid, name text, price decimal, image text, qty int)");

        session.execute("CREATE TABLE IF NOT EXISTS " + keyspace + ".orders ("
                + "username text, order_id timeuuid, total decimal, status text,"
                + " created_at timestamp, ship_to text, card_last4 text,"
                + " items list<frozen<order_item>>," + " PRIMARY KEY (username, order_id))"
                + " WITH CLUSTERING ORDER BY (order_id DESC)");
    }

    /** Creates the product index with an explicit mapping if it does not exist yet. */
    public void createOpenSearchIndex() {
        if (openSearch.head("/" + index) == 200) {
            return;
        }
        String mapping = """
                {
                  "mappings": {
                    "properties": {
                      "name":        { "type": "text", "fields": { "keyword": { "type": "keyword" } } },
                      "description": { "type": "text" },
                      "category":    { "type": "keyword" },
                      "tags":        { "type": "keyword" },
                      "species":     { "type": "text", "fields": { "keyword": { "type": "keyword" } } },
                      "price":       { "type": "double" },
                      "image":       { "type": "keyword" },
                      "stock":       { "type": "integer" }
                    }
                  }
                }
                """;
        openSearch.put("/" + index, mapping);
    }
}
