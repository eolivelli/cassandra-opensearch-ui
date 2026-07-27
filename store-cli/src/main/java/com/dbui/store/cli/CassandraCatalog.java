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
import com.dbui.store.cli.ProductIngestCli.ParsedProduct;
import java.time.Instant;

/**
 * Writes products into Cassandra. The schema mirrors the one created by the store application's
 * {@code SchemaInitializer} so the CLI can be run before the app has ever started. Every write goes
 * to both {@code products} (point look-ups by id) and {@code products_by_category} (category
 * browse) to keep the two query tables consistent.
 */
final class CassandraCatalog {

    private final CqlSession session;
    private final String keyspace;
    private final String indexName;
    private final boolean directWriteToOpenSearch;

    CassandraCatalog(CqlSession session, String keyspace, String indexName,
            boolean directWriteToOpenSearch) {
        this.session = session;
        this.keyspace = keyspace;
        this.indexName = indexName;
        this.directWriteToOpenSearch = directWriteToOpenSearch;
    }

    void ensureSchema() {
        session.execute("CREATE KEYSPACE IF NOT EXISTS " + keyspace
                + " WITH replication = {'class':'SimpleStrategy','replication_factor':1}");
        session.execute("CREATE TABLE IF NOT EXISTS " + keyspace + ".products ("
                + "id uuid PRIMARY KEY, name text, description text, category text,"
                + " tags set<text>, price decimal, image text, species text, stock int,"
                + " created_at timestamp)");

        if (!directWriteToOpenSearch) {
            // Create the custom OpenSearchIndex in Cassandra, and Cassandra will write to
            // OpenSearch
            // when the data is written to Cassandra
            session.execute("CREATE CUSTOM INDEX IF NOT EXISTS h2o_products ON " + keyspace
                    + ".products ()"
                    + "USING 'OpenSearchIndex' with OPTIONS = {'createIndexIfNotExists':'false', 'indexName':'"
                    + indexName + "'}");
        }

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
                + " items list<frozen<order_item>>,"
                + " PRIMARY KEY (username, order_id)) WITH CLUSTERING ORDER BY (order_id DESC)");
    }

    void truncate() {
        session.execute("TRUNCATE " + keyspace + ".products");
        session.execute("TRUNCATE " + keyspace + ".products_by_category");
    }

    PreparedStatement prepareInsertProduct() {
        return session.prepare("INSERT INTO " + keyspace + ".products"
                + " (id, name, description, category, tags, price, image, species, stock, created_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
    }

    PreparedStatement prepareInsertByCategory() {
        return session.prepare("INSERT INTO " + keyspace + ".products_by_category"
                + " (category, name, id, description, tags, price, image, species, stock)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
    }

    /**
     * Writes a product to both query tables. Note: {@code products_by_category} is keyed by
     * {@code (category, name, id)}, so re-ingesting a product whose name or category changed (with
     * the same id) leaves the old browse row behind. Run with {@code --recreate} when product
     * identity fields change; {@code products} (keyed by id) always stays correct.
     */
    void insert(PreparedStatement insertProduct, PreparedStatement insertByCategory,
            ParsedProduct p) {
        session.execute(insertProduct.bind(p.id(), p.name(), p.description(), p.category(),
                p.tags(), p.price(), p.image(), p.species(), p.stock(), Instant.now()));
        session.execute(insertByCategory.bind(p.category(), p.name(), p.id(), p.description(),
                p.tags(), p.price(), p.image(), p.species(), p.stock()));
    }
}
