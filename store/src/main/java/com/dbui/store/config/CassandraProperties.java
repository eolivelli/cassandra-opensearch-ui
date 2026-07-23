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
package com.dbui.store.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings for the Cassandra cluster. Defaults target the local Cassandra started by the
 * repository's docker-compose.yml (shared with the DB UI module).
 */
@ConfigurationProperties(prefix = "store.cassandra")
public class CassandraProperties {

    /** Contact point host. */
    private String host = "127.0.0.1";

    /** Native protocol port. */
    private int port = 9042;

    /** Local datacenter name; Cassandra's default single-node DC is "datacenter1". */
    private String localDatacenter = "datacenter1";

    /** Keyspace that holds the store's tables. */
    private String keyspace = "h2o_store";

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

    public String getKeyspace() {
        return keyspace;
    }

    public void setKeyspace(String keyspace) {
        this.keyspace = keyspace;
    }
}
