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

import com.dbui.history.QueryHistoryService;
import com.dbui.model.OsDocument;
import com.dbui.model.OsDocumentsResult;
import com.dbui.model.OsIndexSchema;
import com.dbui.model.OsIndicesResult;
import com.dbui.model.OsQueryRequest;
import com.dbui.model.OsQueryResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for browsing OpenSearch and running ad-hoc REST calls. */
@RestController
@RequestMapping("/api/opensearch")
public class OpenSearchController {

    private final OpenSearchService service;
    private final QueryHistoryService history;

    public OpenSearchController(OpenSearchService service, QueryHistoryService history) {
        this.service = service;
        this.history = history;
    }

    @GetMapping("/indices")
    public OsIndicesResult indices() {
        return service.listIndices();
    }

    @GetMapping("/indices/{index}/documents")
    public OsDocumentsResult documents(@PathVariable String index,
            @RequestParam(defaultValue = "50") int size) {
        return service.indexDocuments(index, size);
    }

    @GetMapping("/indices/{index}/documents/{id}")
    public OsDocument document(@PathVariable String index, @PathVariable String id) {
        return service.getDocument(index, id);
    }

    @GetMapping("/indices/{index}/schema")
    public OsIndexSchema schema(@PathVariable String index) {
        return service.indexSchema(index);
    }

    /**
     * Runs an arbitrary OpenSearch REST call. The command is recorded in the history before it
     * runs, so even a call that fails can be recalled and corrected.
     */
    @PostMapping("/query")
    public OsQueryResult query(@RequestBody OsQueryRequest request) {
        String path = request.path() == null ? "" : request.path().strip();
        if (path.isEmpty()) {
            throw new IllegalArgumentException("No request path provided");
        }
        String method = request.method() == null || request.method().isBlank()
                ? "GET"
                : request.method().strip().toUpperCase(java.util.Locale.ROOT);
        history.recordOpenSearch(method, path, request.body());
        return service.executeRequest(method, path, request.body());
    }
}
