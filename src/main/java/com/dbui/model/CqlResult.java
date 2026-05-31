package com.dbui.model;

import java.util.List;
import java.util.Map;

/**
 * Uniform tabular result for any Cassandra query.
 *
 * @param cql      the exact CQL statement that produced this result (shown in the UI)
 * @param columns  ordered column names
 * @param rows     row data, each row a column-name to value map
 * @param rowCount number of returned rows
 */
public record CqlResult(
        String cql,
        List<String> columns,
        List<Map<String, Object>> rows,
        int rowCount) {

    public static CqlResult of(String cql, List<String> columns, List<Map<String, Object>> rows) {
        return new CqlResult(cql, columns, rows, rows.size());
    }
}
