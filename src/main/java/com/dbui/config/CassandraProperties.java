package com.dbui.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings for the Cassandra cluster. Defaults target a local
 * Cassandra running in Docker (see docker-compose.yml).
 */
@ConfigurationProperties(prefix = "dbui.cassandra")
public class CassandraProperties {

    /** Contact point host. */
    private String host = "127.0.0.1";

    /** Native protocol port. */
    private int port = 9042;

    /** Local datacenter name; Cassandra's default single-node DC is "datacenter1". */
    private String localDatacenter = "datacenter1";

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getLocalDatacenter() {
        return localDatacenter;
    }

    public void setLocalDatacenter(String localDatacenter) {
        this.localDatacenter = localDatacenter;
    }
}
