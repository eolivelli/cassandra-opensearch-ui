package com.dbui.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Schema of an OpenSearch index, as returned by {@code GET /{index}}.
 *
 * @param request  the REST call used
 * @param mappings the index field mappings
 * @param settings the index settings
 * @param aliases  the index aliases
 */
public record OsIndexSchema(OsRequest request, JsonNode mappings, JsonNode settings, JsonNode aliases) {
}
