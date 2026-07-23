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
package com.dbui.opensearch;

import com.dbui.model.OsDocument;
import com.dbui.model.OsDocumentsResult;
import com.dbui.model.OsIndexSchema;
import com.dbui.model.OsIndicesResult;
import com.dbui.model.OsQueryResult;
import com.dbui.model.OsRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Read-only browsing of OpenSearch indices and their documents. */
@Service
public class OpenSearchService {

    /** Upper bound on how many documents a single request may return. */
    public static final int MAX_SIZE = 1000;

    private final OpenSearchClient client;
    private final ObjectMapper mapper;

    public OpenSearchService(OpenSearchClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    /** Lists all indices using the _cat API in JSON form. */
    public OsIndicesResult listIndices() {
        String path = "/_cat/indices?format=json&s=index";
        OsRequest request = OsRequest.get(path);
        JsonNode response = client.get(path);

        List<Map<String, Object>> indices = new ArrayList<>();
        if (response instanceof ArrayNode array) {
            for (JsonNode node : array) {
                indices.add(mapper.convertValue(node, MAP_TYPE));
            }
        }
        return new OsIndicesResult(request, indices);
    }

    /** Fetches up to {@code size} documents from an index using match_all. */
    public OsDocumentsResult indexDocuments(String index, int size) {
        int effectiveSize = Math.min(Math.max(size, 1), MAX_SIZE);
        String path = "/" + index + "/_search";
        ObjectNode body = mapper.createObjectNode();
        body.put("size", effectiveSize);
        body.put("track_total_hits", true);
        body.set("query", mapper.createObjectNode().set("match_all", mapper.createObjectNode()));
        String bodyJson = body.toPrettyString();

        OsRequest request = OsRequest.post(path, bodyJson);
        JsonNode response = client.post(path, bodyJson);

        long total = response.path("hits").path("total").path("value").asLong(0);

        List<Map<String, Object>> documents = new ArrayList<>();
        Set<String> columns = new LinkedHashSet<>();
        columns.add("_id");

        JsonNode hits = response.path("hits").path("hits");
        if (hits instanceof ArrayNode hitArray) {
            for (JsonNode hit : hitArray) {
                Map<String, Object> doc = new LinkedHashMap<>();
                doc.put("_id", hit.path("_id").asText());
                JsonNode source = hit.path("_source");
                if (source instanceof ObjectNode sourceObj) {
                    Map<String, Object> sourceMap = mapper.convertValue(sourceObj, MAP_TYPE);
                    columns.addAll(sourceMap.keySet());
                    doc.putAll(sourceMap);
                }
                documents.add(doc);
            }
        }
        return new OsDocumentsResult(request, total, new ArrayList<>(columns), documents);
    }

    /**
     * Runs an arbitrary REST call entered on the query page. The {@code path} is taken relative to
     * the configured base URL (a leading slash is added when missing); an absolute {@code http(s)}
     * URL is used as-is. Returns the HTTP status and response body even for non-2xx responses.
     */
    public OsQueryResult executeRequest(String method, String path, String body) {
        String verb = (method == null || method.isBlank())
                ? "GET"
                : method.strip().toUpperCase(Locale.ROOT);
        String rawPath = path == null ? "" : path.strip();
        String effectivePath = rawPath.startsWith("http://") || rawPath.startsWith("https://")
                || rawPath.startsWith("/") ? rawPath : "/" + rawPath;
        String effectiveBody = body == null || body.isBlank() ? null : body;

        OsRequest request = new OsRequest(verb, effectivePath, effectiveBody);
        OpenSearchClient.ClientResponse response = client.request(verb, effectivePath,
                effectiveBody);
        return new OsQueryResult(request, response.status(), response.body());
    }

    /** Returns the mappings, settings and aliases of an index. */
    public OsIndexSchema indexSchema(String index) {
        String path = "/" + index;
        OsRequest request = OsRequest.get(path);
        JsonNode response = client.get(path);

        // GET /{index} returns { "<actual-index-name>": { aliases, mappings, settings } }.
        JsonNode body = response.isObject() && response.size() > 0
                ? response.fields().next().getValue()
                : response;
        return new OsIndexSchema(request, body.path("mappings"), body.path("settings"),
                body.path("aliases"));
    }

    /** Fetches a single document by id. */
    public OsDocument getDocument(String index, String id) {
        String path = "/" + index + "/_doc/" + id;
        OsRequest request = OsRequest.get(path);
        JsonNode response = client.get(path);
        boolean found = response.path("found").asBoolean(false);
        return new OsDocument(request, id, found, response.path("_source"));
    }

    private static final com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new com.fasterxml.jackson.core.type.TypeReference<>() {
    };
}
