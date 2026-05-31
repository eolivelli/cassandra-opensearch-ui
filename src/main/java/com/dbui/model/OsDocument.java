package com.dbui.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A single OpenSearch document, as returned by {@code GET /{index}/_doc/{id}}.
 *
 * @param request the REST call used
 * @param id      the document id
 * @param found   whether the document exists
 * @param source  the document {@code _source}
 */
public record OsDocument(OsRequest request, String id, boolean found, JsonNode source) {
}
