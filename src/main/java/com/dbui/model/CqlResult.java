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
package com.dbui.model;

import java.util.List;
import java.util.Map;

/**
 * Uniform tabular result for any Cassandra query.
 *
 * @param cql
 *            the exact CQL statement that produced this result (shown in the UI)
 * @param columns
 *            ordered column names
 * @param rows
 *            row data, each row a column-name to value map
 * @param rowCount
 *            number of returned rows
 */
public record CqlResult(String cql, List<String> columns, List<Map<String, Object>> rows,
        int rowCount) {

    public static CqlResult of(String cql, List<String> columns, List<Map<String, Object>> rows) {
        return new CqlResult(cql, columns, rows, rows.size());
    }
}
