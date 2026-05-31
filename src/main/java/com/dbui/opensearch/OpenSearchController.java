package com.dbui.opensearch;

import com.dbui.model.OsDocument;
import com.dbui.model.OsDocumentsResult;
import com.dbui.model.OsIndexSchema;
import com.dbui.model.OsIndicesResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for browsing OpenSearch. */
@RestController
@RequestMapping("/api/opensearch")
public class OpenSearchController {

    private final OpenSearchService service;

    public OpenSearchController(OpenSearchService service) {
        this.service = service;
    }

    @GetMapping("/indices")
    public OsIndicesResult indices() {
        return service.listIndices();
    }

    @GetMapping("/indices/{index}/documents")
    public OsDocumentsResult documents(
            @PathVariable String index,
            @RequestParam(defaultValue = "50") int size) {
        return service.indexDocuments(index, size);
    }

    @GetMapping("/indices/{index}/documents/{id}")
    public OsDocument document(
            @PathVariable String index,
            @PathVariable String id) {
        return service.getDocument(index, id);
    }

    @GetMapping("/indices/{index}/schema")
    public OsIndexSchema schema(@PathVariable String index) {
        return service.indexSchema(index);
    }
}
