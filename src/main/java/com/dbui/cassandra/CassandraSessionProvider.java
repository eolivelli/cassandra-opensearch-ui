package com.dbui.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.dbui.config.CassandraProperties;
import jakarta.annotation.PreDestroy;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Lazily creates and caches a {@link CqlSession}. The session is only opened on
 * the first request that needs it, so the web application starts cleanly even
 * when Cassandra is not (yet) running.
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
     * Returns a live session, creating it on demand. Throws if Cassandra cannot
     * be reached, so callers can surface a meaningful error to the UI.
     */
    public CqlSession session() {
        CqlSession local = session;
        if (local != null && !local.isClosed()) {
            return local;
        }
        synchronized (this) {
            if (session == null || session.isClosed()) {
                log.info("Opening Cassandra session to {}:{} (dc={})",
                        properties.getHost(), properties.getPort(), properties.getLocalDatacenter());
                session = CqlSession.builder()
                        .addContactPoint(new InetSocketAddress(properties.getHost(), properties.getPort()))
                        .withLocalDatacenter(properties.getLocalDatacenter())
                        .build();
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
