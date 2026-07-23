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
 * Result of an arbitrary CQL statement run from the query page. For a {@code SELECT} the columns
 * and rows are populated; for DDL / DML there are no rows and {@code applied} reflects
 * {@code ResultSet.wasApplied()} (relevant for lightweight transactions).
 *
 * @param cql
 *            the exact CQL statement that was executed
 * @param columns
 *            ordered column names (empty for statements that return no rows)
 * @param rows
 *            row data, each row a column-name to value map (all rows, no paging)
 * @param rowCount
 *            number of returned rows
 * @param applied
 *            whether the statement was applied (always true for plain writes; may be false for a
 *            failed conditional/LWT statement)
 */
public record CqlQueryResult(String cql, List<String> columns, List<Map<String, Object>> rows,
        int rowCount, boolean applied) {
}
