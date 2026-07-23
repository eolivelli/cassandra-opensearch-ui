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

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A single OpenSearch document, as returned by {@code GET /{index}/_doc/{id}}.
 *
 * @param request
 *            the REST call used
 * @param id
 *            the document id
 * @param found
 *            whether the document exists
 * @param source
 *            the document {@code _source}
 */
public record OsDocument(OsRequest request, String id, boolean found, JsonNode source) {
}
