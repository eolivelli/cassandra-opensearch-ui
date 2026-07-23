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
package com.dbui.cassandra;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.data.TupleValue;
import com.datastax.oss.driver.api.core.data.UdtValue;
import com.datastax.oss.driver.api.core.metadata.schema.ClusteringOrder;
import com.datastax.oss.driver.api.core.metadata.schema.ColumnMetadata;
import com.datastax.oss.driver.api.core.metadata.schema.IndexMetadata;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.datastax.oss.driver.api.core.metadata.schema.TableMetadata;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.ListType;
import com.datastax.oss.driver.api.core.type.MapType;
import com.datastax.oss.driver.api.core.type.SetType;
import com.datastax.oss.driver.api.core.type.TupleType;
import com.datastax.oss.driver.api.core.type.UserDefinedType;
import com.dbui.model.CassandraSchema;
import com.dbui.model.CqlQueryResult;
import com.dbui.model.CqlResult;
import com.dbui.model.TableRows;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Read-only access to a Cassandra cluster. Every method returns the exact CQL it executed (or, for
 * schema, the CREATE statements) alongside the data, so the UI can show users how to reproduce it.
 */
@Service
public class CassandraService {

    /** Upper bound on how many rows a single table-content request may return. */
    public static final int MAX_ROWS = 1000;

    private final CassandraSessionProvider sessionProvider;

    public CassandraService(CassandraSessionProvider sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    /** Lists all keyspaces with their durable-writes flag and replication settings. */
    public CqlResult listKeyspaces() {
        String cql = "SELECT keyspace_name, durable_writes, replication "
                + "FROM system_schema.keyspaces";
        return execute(cql);
    }

    /** Lists the tables of a keyspace. */
    public CqlResult listTables(String keyspace) {
        String cql = "SELECT table_name FROM system_schema.tables " + "WHERE keyspace_name = '"
                + escape(keyspace) + "'";
        RawResult raw = run(SimpleStatement
                .builder("SELECT table_name FROM system_schema.tables WHERE keyspace_name = ?")
                .addPositionalValue(keyspace).build());
        return CqlResult.of(cql, raw.columns(), raw.rows());
    }

    /** Returns up to {@code limit} rows from a table, with its primary-key columns. */
    public TableRows tableRows(String keyspace, String table, int limit) {
        int effectiveLimit = Math.min(Math.max(limit, 1), MAX_ROWS);
        String cql = "SELECT * FROM " + quote(keyspace) + "." + quote(table) + " LIMIT "
                + effectiveLimit;
        RawResult raw = run(SimpleStatement.newInstance(cql));
        List<String> primaryKey = primaryKeyColumns(keyspace, table);
        return new TableRows(cql, raw.columns(), primaryKey, raw.rows(), raw.rows().size());
    }

    /**
     * Runs an arbitrary CQL statement entered on the query page (SELECT, DDL or DML) and returns
     * all of its rows at once (the driver pages internally, but every page is fetched). For
     * statements that return no rows the columns and rows are empty.
     */
    public CqlQueryResult executeQuery(String cql) {
        RawResult raw = run(SimpleStatement.newInstance(cql));
        return new CqlQueryResult(cql, raw.columns(), raw.rows(), raw.rows().size(), raw.applied());
    }

    /** Builds a schema description (columns, indexes, UDTs) for a table. */
    public CassandraSchema tableSchema(String keyspace, String table) {
        TableMetadata tbl = tableMetadata(keyspace, table);

        Map<String, ClusteringOrder> clustering = new LinkedHashMap<>();
        tbl.getClusteringColumns()
                .forEach((col, order) -> clustering.put(col.getName().asInternal(), order));
        List<String> partition = tbl.getPartitionKey().stream().map(c -> c.getName().asInternal())
                .toList();

        List<CassandraSchema.Column> columns = new ArrayList<>();
        Map<String, UserDefinedType> udts = new LinkedHashMap<>();
        for (ColumnMetadata col : tbl.getColumns().values()) {
            String name = col.getName().asInternal();
            String kind;
            String order = null;
            if (partition.contains(name)) {
                kind = "PARTITION_KEY";
            } else if (clustering.containsKey(name)) {
                kind = "CLUSTERING";
                order = clustering.get(name).name();
            } else if (col.isStatic()) {
                kind = "STATIC";
            } else {
                kind = "REGULAR";
            }
            columns.add(
                    new CassandraSchema.Column(name, col.getType().asCql(true, true), kind, order));
            collectUdts(col.getType(), udts);
        }

        List<CassandraSchema.Index> indexes = new ArrayList<>();
        for (IndexMetadata idx : tbl.getIndexes().values()) {
            String className = idx.getClassName().orElse(null);
            String kind = className != null && className.endsWith("StorageAttachedIndex")
                    ? "SAI"
                    : idx.getKind().name();
            indexes.add(new CassandraSchema.Index(idx.getName().asInternal(), kind, idx.getTarget(),
                    className, new LinkedHashMap<>(idx.getOptions())));
        }

        List<CassandraSchema.Udt> types = udts.values().stream()
                .map(u -> new CassandraSchema.Udt(u.getName().asInternal(), u.describe(true)))
                .toList();

        return new CassandraSchema(keyspace, table, tbl.describeWithChildren(true), columns,
                indexes, types);
    }

    private List<String> primaryKeyColumns(String keyspace, String table) {
        TableMetadata tbl = tableMetadata(keyspace, table);
        List<String> pk = new ArrayList<>();
        tbl.getPartitionKey().forEach(c -> pk.add(c.getName().asInternal()));
        tbl.getClusteringColumns().keySet().forEach(c -> pk.add(c.getName().asInternal()));
        return pk;
    }

    private TableMetadata tableMetadata(String keyspace, String table) {
        CqlSession session = sessionProvider.session();
        KeyspaceMetadata ks = session.getMetadata()
                .getKeyspace(CqlIdentifier.fromInternal(keyspace))
                .orElseThrow(() -> new IllegalArgumentException("Keyspace not found: " + keyspace));
        return ks.getTable(CqlIdentifier.fromInternal(table)).orElseThrow(
                () -> new IllegalArgumentException("Table not found: " + keyspace + "." + table));
    }

    /** Recursively collects user-defined types reachable from a column type. */
    private static void collectUdts(DataType type, Map<String, UserDefinedType> out) {
        switch (type) {
            case UserDefinedType udt -> {
                if (out.putIfAbsent(udt.getName().asInternal(), udt) == null) {
                    udt.getFieldTypes().forEach(t -> collectUdts(t, out));
                }
            }
            case ListType list -> collectUdts(list.getElementType(), out);
            case SetType set -> collectUdts(set.getElementType(), out);
            case MapType map -> {
                collectUdts(map.getKeyType(), out);
                collectUdts(map.getValueType(), out);
            }
            case TupleType tuple -> tuple.getComponentTypes().forEach(t -> collectUdts(t, out));
            default -> {
                // scalar type, nothing to collect
            }
        }
    }

    private CqlResult execute(String cql) {
        RawResult raw = run(SimpleStatement.newInstance(cql));
        return CqlResult.of(cql, raw.columns(), raw.rows());
    }

    private RawResult run(SimpleStatement statement) {
        CqlSession session = sessionProvider.session();
        ResultSet rs = session.execute(statement);
        ColumnDefinitions defs = rs.getColumnDefinitions();

        List<String> columns = new ArrayList<>(defs.size());
        for (ColumnDefinition def : defs) {
            columns.add(def.getName().asInternal());
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Row row : rs) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                values.put(columns.get(i), toJsonValue(row.getObject(i)));
            }
            rows.add(values);
        }
        return new RawResult(columns, rows, rs.wasApplied());
    }

    private record RawResult(List<String> columns, List<Map<String, Object>> rows,
            boolean applied) {
    }

    /** Converts a driver value to something Jackson can render predictably. */
    static Object toJsonValue(Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof ByteBuffer bb) {
            ByteBuffer dup = bb.duplicate();
            byte[] bytes = new byte[dup.remaining()];
            dup.get(bytes);
            StringBuilder hex = new StringBuilder("0x");
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        }
        if (value instanceof UdtValue udt) {
            Map<String, Object> map = new LinkedHashMap<>();
            udt.getType().getFieldNames()
                    .forEach(name -> map.put(name.asInternal(), toJsonValue(udt.getObject(name))));
            return map;
        }
        if (value instanceof TupleValue tuple) {
            List<Object> list = new ArrayList<>();
            int size = tuple.getType().getComponentTypes().size();
            for (int i = 0; i < size; i++) {
                list.add(toJsonValue(tuple.getObject(i)));
            }
            return list;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            map.forEach((k, v) -> out.put(String.valueOf(k), toJsonValue(v)));
            return out;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(CassandraService::toJsonValue).toList();
        }
        return String.valueOf(value);
    }

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static String escape(String literal) {
        return literal.replace("'", "''");
    }
}
