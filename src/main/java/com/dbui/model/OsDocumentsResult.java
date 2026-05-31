package com.dbui.model;

import java.util.List;
import java.util.Map;

/**
 * Result of fetching documents from an OpenSearch index.
 *
 * @param request   the REST call used (POST /{index}/_search)
 * @param total     total number of matching documents in the index
 * @param columns   union of {@code _source} field names across the returned docs,
 *                  preceded by {@code _id}; used for tabular display
 * @param documents one map per hit, containing {@code _id} plus the flattened
 *                  {@code _source} fields
 */
public record OsDocumentsResult(
        OsRequest request,
        long total,
        List<String> columns,
        List<Map<String, Object>> documents) {
}
