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

import com.datastax.oss.driver.api.core.CqlSession;
import com.dbui.store.config.CassandraProperties;
import jakarta.annotation.PreDestroy;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Lazily creates and caches a {@link CqlSession}. The session is opened on first use, so the web
 * application starts cleanly even when Cassandra is not (yet) running; the schema is created on the
 * first successful connection by {@link SchemaInitializer}.
 */
@Component
public class CassandraSessionProvider {

    private static final Logger log = LoggerFactory.getLogger(CassandraSessionProvider.class);

    private final CassandraProperties properties;
    private volatile CqlSession session;

    public CassandraSessionProvider(CassandraProperties properties) {
        this.properties = properties;
    }

    /**
     * Returns a live session, creating it on demand. Throws if Cassandra cannot be reached so
     * callers can surface a meaningful error to the UI.
     */
    public CqlSession session() {
        CqlSession local = session;
        if (local != null && !local.isClosed()) {
            return local;
        }
        synchronized (this) {
            if (session == null || session.isClosed()) {
                log.info("Opening Cassandra session to {}:{} (dc={})", properties.getHost(),
                        properties.getPort(), properties.getLocalDatacenter());
                session = CqlSession.builder()
                        .addContactPoint(
                                new InetSocketAddress(properties.getHost(), properties.getPort()))
                        .withLocalDatacenter(properties.getLocalDatacenter()).build();
            }
            return session;
        }
    }

    @PreDestroy
    public void close() {
        CqlSession local = session;
        if (local != null && !local.isClosed()) {
            local.close();
        }
    }
}
