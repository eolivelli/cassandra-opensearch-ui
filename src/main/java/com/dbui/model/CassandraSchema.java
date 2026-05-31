package com.dbui.model;

import java.util.List;
import java.util.Map;

/**
 * Schema description of a Cassandra table: its columns and primary key, its
 * (secondary / SAI) indexes, and the definitions of any user-defined types it
 * uses. {@code createStatement} is the full CREATE TABLE (with its indexes) as
 * produced by the driver.
 */
public record CassandraSchema(
        String keyspace,
        String table,
        String createStatement,
        List<Column> columns,
        List<Index> indexes,
        List<Udt> types) {

    /**
     * @param name            column name
     * @param type            CQL type
     * @param kind            PARTITION_KEY, CLUSTERING, STATIC or REGULAR
     * @param clusteringOrder ASC/DESC for clustering columns, otherwise {@code null}
     */
    public record Column(String name, String type, String kind, String clusteringOrder) {
    }

    /**
     * @param name      index name
     * @param kind      friendly kind ("SAI" for storage-attached, otherwise the driver kind)
     * @param target    indexed column or expression
     * @param className implementation class for custom indexes, otherwise {@code null}
     * @param options   raw index options
     */
    public record Index(String name, String kind, String target, String className,
                        Map<String, String> options) {
    }

    /**
     * @param name            type name
     * @param createStatement the CREATE TYPE statement
     */
    public record Udt(String name, String createStatement) {
    }
}
