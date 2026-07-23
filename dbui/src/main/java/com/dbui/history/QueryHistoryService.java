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
package com.dbui.history;

import com.dbui.config.HistoryProperties;
import com.dbui.model.HistoryEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Keeps a history of executed query commands (never their results) so users can re-run them. The
 * history is persisted to a local JSON file and reloaded on startup. Newest commands come first,
 * and re-running an identical command moves it back to the top rather than duplicating it.
 */
@Service
public class QueryHistoryService {

    private static final Logger log = LoggerFactory.getLogger(QueryHistoryService.class);

    private static final TypeReference<List<HistoryEntry>> LIST_TYPE = new TypeReference<>() {
    };

    private final HistoryProperties properties;
    private final ObjectMapper mapper;
    private final Path file;

    /** All entries, newest first, across both sources. Guarded by {@code this}. */
    private final List<HistoryEntry> entries = new ArrayList<>();

    public QueryHistoryService(HistoryProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        this.file = Path.of(properties.getFile());
    }

    @PostConstruct
    synchronized void load() {
        if (!Files.isReadable(file)) {
            return;
        }
        try {
            List<HistoryEntry> loaded = mapper.readValue(Files.readAllBytes(file), LIST_TYPE);
            entries.addAll(loaded);
            log.info("Loaded {} query-history entries from {}", entries.size(), file);
        } catch (IOException e) {
            log.warn("Could not read query history from {}: {}", file, e.getMessage());
        }
    }

    /** Records a Cassandra command; returns the stored entry. */
    public HistoryEntry recordCassandra(String cql) {
        return record(HistoryEntry.cassandra(cql, now()));
    }

    /** Records an OpenSearch command; returns the stored entry. */
    public HistoryEntry recordOpenSearch(String method, String path, String body) {
        return record(HistoryEntry.opensearch(method, path, body, now()));
    }

    /** Returns the history for a source ({@code "cassandra"} or {@code "opensearch"}). */
    public synchronized List<HistoryEntry> list(String type) {
        List<HistoryEntry> result = new ArrayList<>();
        for (HistoryEntry entry : entries) {
            if (entry.type().equals(type)) {
                result.add(entry);
            }
        }
        return result;
    }

    /** Clears the history for a single source, leaving the other source untouched. */
    public synchronized void clear(String type) {
        entries.removeIf(entry -> entry.type().equals(type));
        persist();
    }

    private synchronized HistoryEntry record(HistoryEntry entry) {
        entries.removeIf(existing -> existing.commandKey().equals(entry.commandKey()));
        entries.add(0, entry);
        trim(entry.type());
        persist();
        return entry;
    }

    /** Caps the number of entries kept for a source at {@code maxEntries}. */
    private void trim(String type) {
        int kept = 0;
        for (int i = 0; i < entries.size();) {
            if (entries.get(i).type().equals(type) && ++kept > properties.getMaxEntries()) {
                entries.remove(i);
            } else {
                i++;
            }
        }
    }

    private void persist() {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), entries);
        } catch (IOException e) {
            log.warn("Could not write query history to {}: {}", file, e.getMessage());
        }
    }

    private static String now() {
        return Instant.now().toString();
    }
}
