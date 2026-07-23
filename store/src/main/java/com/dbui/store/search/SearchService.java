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
package com.dbui.store.search;

import com.dbui.store.config.OpenSearchProperties;
import com.dbui.store.opensearch.OpenSearchClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * The site's free-text product search - the one and only feature powered by OpenSearch. The rest of
 * the store reads from Cassandra; here we query the {@code h2o_products} index (kept in sync by the
 * ingest CLI) to get relevance ranking, fuzziness and multi-field matching that Cassandra does not
 * provide.
 */
@Service
public class SearchService {

    private final OpenSearchClient client;
    private final ObjectMapper mapper;
    private final String index;

    public SearchService(OpenSearchClient client, ObjectMapper mapper,
            OpenSearchProperties properties) {
        this.client = client;
        this.mapper = mapper;
        this.index = properties.getIndex();
    }

    /**
     * Runs a search. A blank query matches everything (useful for a category-only browse of the
     * search page); a category, when given, is applied as a filter.
     */
    public SearchResponse search(String query, String category, int size) {
        String q = query == null ? "" : query.strip();
        ObjectNode body = buildQuery(q, category, size);
        JsonNode response = client.post("/" + index + "/_search", body.toString());

        long took = response.path("took").asLong(0);
        long total = response.path("hits").path("total").path("value").asLong(0);

        List<SearchResponse.Hit> hits = new ArrayList<>();
        for (JsonNode hit : response.path("hits").path("hits")) {
            JsonNode source = hit.path("_source");
            hits.add(new SearchResponse.Hit(hit.path("_id").asText(),
                    source.path("name").asText(null), source.path("description").asText(null),
                    source.path("category").asText(null), readTags(source.path("tags")),
                    source.has("price") ? new BigDecimal(source.path("price").asText("0")) : null,
                    source.path("image").asText(null),
                    source.hasNonNull("species") ? source.path("species").asText() : null,
                    hit.path("_score").asDouble(0)));
        }
        return new SearchResponse(q, total, took, hits);
    }

    private ObjectNode buildQuery(String q, String category, int size) {
        ObjectNode body = mapper.createObjectNode();
        body.put("size", size);

        ObjectNode bool = mapper.createObjectNode();
        ArrayNode must = mapper.createArrayNode();
        if (q.isBlank()) {
            must.add(mapper.createObjectNode().set("match_all", mapper.createObjectNode()));
        } else {
            ObjectNode multiMatch = mapper.createObjectNode();
            multiMatch.put("query", q);
            ArrayNode fields = mapper.createArrayNode();
            // Boost the most identifying fields; description is matched but weighted lightly.
            fields.add("name^3").add("species^2").add("tags^2").add("description");
            multiMatch.set("fields", fields);
            multiMatch.put("fuzziness", "AUTO");
            must.add(mapper.createObjectNode().set("multi_match", multiMatch));
        }
        bool.set("must", must);

        if (category != null && !category.isBlank()) {
            ArrayNode filter = mapper.createArrayNode();
            ObjectNode term = mapper.createObjectNode();
            term.set("term", mapper.createObjectNode().put("category", category));
            filter.add(term);
            bool.set("filter", filter);
        }

        body.set("query", mapper.createObjectNode().set("bool", bool));
        return body;
    }

    private List<String> readTags(JsonNode tagsNode) {
        List<String> tags = new ArrayList<>();
        if (tagsNode.isArray()) {
            for (JsonNode tag : tagsNode) {
                tags.add(tag.asText());
            }
        } else if (tagsNode.isTextual()) {
            tags.add(tagsNode.asText());
        }
        return tags;
    }
}
