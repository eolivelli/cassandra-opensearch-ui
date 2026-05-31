package com.dbui.model;

import java.util.List;
import java.util.Map;

/**
 * Rows of a Cassandra table, plus the table's primary-key column names so the UI
 * can build a "fetch this single row" CQL when a row is opened in detail.
 *
 * @param cql        the SELECT that produced these rows
 * @param columns    ordered column names
 * @param primaryKey ordered primary-key columns (partition key first, then clustering)
 * @param rows       row data
 * @param rowCount   number of returned rows
 */
public record TableRows(
        String cql,
        List<String> columns,
        List<String> primaryKey,
        List<Map<String, Object>> rows,
        int rowCount) {
}
