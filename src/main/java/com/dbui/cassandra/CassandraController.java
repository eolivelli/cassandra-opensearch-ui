package com.dbui.cassandra;

import com.dbui.model.CassandraSchema;
import com.dbui.model.CqlResult;
import com.dbui.model.TableRows;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for browsing Cassandra. */
@RestController
@RequestMapping("/api/cassandra")
public class CassandraController {

    private final CassandraService service;

    public CassandraController(CassandraService service) {
        this.service = service;
    }

    @GetMapping("/keyspaces")
    public CqlResult keyspaces() {
        return service.listKeyspaces();
    }

    @GetMapping("/keyspaces/{keyspace}/tables")
    public CqlResult tables(@PathVariable String keyspace) {
        return service.listTables(keyspace);
    }

    @GetMapping("/keyspaces/{keyspace}/tables/{table}/rows")
    public TableRows rows(
            @PathVariable String keyspace,
            @PathVariable String table,
            @RequestParam(defaultValue = "100") int limit) {
        return service.tableRows(keyspace, table, limit);
    }

    @GetMapping("/keyspaces/{keyspace}/tables/{table}/schema")
    public CassandraSchema schema(
            @PathVariable String keyspace,
            @PathVariable String table) {
        return service.tableSchema(keyspace, table);
    }
}
