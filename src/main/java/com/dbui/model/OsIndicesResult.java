package com.dbui.model;

import java.util.List;
import java.util.Map;

/**
 * Result of listing OpenSearch indices.
 *
 * @param request the REST call used (GET /_cat/indices)
 * @param indices one map per index, with cat-API fields such as index, health,
 *                status, docs.count and store.size
 */
public record OsIndicesResult(OsRequest request, List<Map<String, Object>> indices) {
}
