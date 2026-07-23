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
package com.dbui;

import org.opensearch.testcontainers.OpenSearchContainer;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests: it launches a real Cassandra 5 and a real OpenSearch 3.x via
 * Testcontainers. The containers are started once (singleton pattern) and shared by every
 * integration test in the JVM; Testcontainers' Ryuk sidecar stops them when the JVM exits.
 */
public abstract class AbstractIntegrationTest {

    private static final String OPENSEARCH_IMAGE = System.getProperty("opensearch.image",
            "opensearchproject/opensearch:3.6.0");

    protected static final CassandraContainer CASSANDRA;
    protected static final OpenSearchContainer<?> OPENSEARCH;

    static {
        CASSANDRA = new CassandraContainer("cassandra:5.0");
        OPENSEARCH = new OpenSearchContainer<>(DockerImageName.parse(OPENSEARCH_IMAGE));
        CASSANDRA.start();
        OPENSEARCH.start();
    }
}
