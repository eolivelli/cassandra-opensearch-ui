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

import static org.assertj.core.api.Assertions.assertThat;

import com.dbui.AbstractIntegrationTest;
import com.dbui.config.OpenSearchProperties;
import com.dbui.model.OsDocumentsResult;
import com.dbui.model.OsIndicesResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link OpenSearchService} against a real OpenSearch container launched by
 * Testcontainers (see {@link AbstractIntegrationTest}).
 */
class OpenSearchServiceTest extends AbstractIntegrationTest {

    static final ObjectMapper MAPPER = new ObjectMapper();
    static OpenSearchService service;

    @BeforeAll
    static void setUp() {
        OpenSearchProperties props = new OpenSearchProperties();
        props.setUrl(OPENSEARCH.getHttpHostAddress());

        OpenSearchClient client = new OpenSearchClient(props, MAPPER);
        service = new OpenSearchService(client, MAPPER);

        // Seed two documents and refresh so they are immediately searchable.
        client.post("/products/_doc?refresh=true",
                "{\"name\":\"Keyboard\",\"price\":49.9,\"in_stock\":true}");
        client.post("/products/_doc?refresh=true",
                "{\"name\":\"Mouse\",\"price\":19.5,\"in_stock\":false}");
    }

    @Test
    void listsIndicesIncludingSeededIndex() {
        OsIndicesResult result = service.listIndices();

        assertThat(result.request().method()).isEqualTo("GET");
        assertThat(result.request().path()).contains("/_cat/indices");

        List<String> names = result.indices().stream().map(i -> (String) i.get("index")).toList();
        assertThat(names).contains("products");
    }

    @Test
    void returnsDocumentsWithColumnsAndRequest() {
        OsDocumentsResult result = service.indexDocuments("products", 50);

        assertThat(result.request().method()).isEqualTo("POST");
        assertThat(result.request().path()).isEqualTo("/products/_search");
        assertThat(result.request().body()).contains("match_all");
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.columns()).contains("_id", "name", "price", "in_stock");

        List<String> names = result.documents().stream().map(d -> (String) d.get("name")).toList();
        assertThat(names).containsExactlyInAnyOrder("Keyboard", "Mouse");
    }

    @Test
    void honoursSizeLimit() {
        OsDocumentsResult result = service.indexDocuments("products", 1);

        assertThat(result.documents()).hasSize(1);
        assertThat(result.total()).isEqualTo(2);
        Map<String, Object> doc = result.documents().get(0);
        assertThat(doc).containsKey("_id");
    }
}
