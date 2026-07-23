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
 * Result of fetching documents from an OpenSearch index.
 *
 * @param request
 *            the REST call used (POST /{index}/_search)
 * @param total
 *            total number of matching documents in the index
 * @param columns
 *            union of {@code _source} field names across the returned docs, preceded by
 *            {@code _id}; used for tabular display
 * @param documents
 *            one map per hit, containing {@code _id} plus the flattened {@code _source} fields
 */
public record OsDocumentsResult(OsRequest request, long total, List<String> columns,
        List<Map<String, Object>> documents) {
}
