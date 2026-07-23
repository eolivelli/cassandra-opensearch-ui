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

import com.dbui.store.cli.ProductIngestCli.ParsedProduct;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Indexes products into OpenSearch over the raw REST API using the JDK {@link HttpClient} - the
 * same approach the web modules take. The mapping matches the store application's
 * {@code SchemaInitializer}.
 */
final class OpenSearchCatalog {

    private static final String MAPPING = """
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

    private final String baseUrl;
    private final String index;
    private final ObjectMapper mapper;
    private final HttpClient http;

    OpenSearchCatalog(String baseUrl, String index, ObjectMapper mapper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.index = index;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    void ensureIndex() {
        int status = send("HEAD", "/" + index, null).statusCode();
        if (status == 200) {
            return;
        }
        HttpResponse<String> response = send("PUT", "/" + index, MAPPING);
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Failed to create index: HTTP " + response.statusCode()
                    + " " + response.body());
        }
    }

    void index(ParsedProduct p) {
        ObjectNode doc = mapper.createObjectNode();
        doc.put("name", p.name());
        doc.put("description", p.description());
        doc.put("category", p.category());
        ArrayNode tags = doc.putArray("tags");
        p.tags().forEach(tags::add);
        doc.put("price", p.price());
        doc.put("image", p.image());
        if (p.species() != null) {
            doc.put("species", p.species());
        }
        doc.put("stock", p.stock());

        HttpResponse<String> response = send("PUT", "/" + index + "/_doc/" + p.id(),
                doc.toString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Failed to index " + p.name() + ": HTTP "
                    + response.statusCode() + " " + response.body());
        }
    }

    /**
     * Deletes the index entirely (mapping included). Used by {@code --recreate} so a stale or
     * auto-detected mapping is rebuilt from {@link #MAPPING} on the next {@link #ensureIndex()}. A
     * missing index (404) is not an error.
     */
    void deleteIndex() {
        HttpResponse<String> response = send("DELETE", "/" + index, null);
        int status = response.statusCode();
        if (status >= 400 && status != 404) {
            throw new IllegalStateException(
                    "Failed to delete index: HTTP " + status + " " + response.body());
        }
    }

    void refresh() {
        HttpResponse<String> response = send("POST", "/" + index + "/_refresh", null);
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Failed to refresh index: HTTP " + response.statusCode()
                    + " " + response.body());
        }
    }

    private HttpResponse<String> send(String method, String path, String body) {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30)).header("Accept", "application/json")
                .method(method, publisher);
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        try {
            return http.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("OpenSearch request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling OpenSearch", e);
        }
    }
}
