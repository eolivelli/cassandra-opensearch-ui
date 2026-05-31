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
    public TableRows rows(@PathVariable String keyspace, @PathVariable String table,
            @RequestParam(defaultValue = "100") int limit) {
        return service.tableRows(keyspace, table, limit);
    }

    @GetMapping("/keyspaces/{keyspace}/tables/{table}/schema")
    public CassandraSchema schema(@PathVariable String keyspace, @PathVariable String table) {
        return service.tableSchema(keyspace, table);
    }
}
