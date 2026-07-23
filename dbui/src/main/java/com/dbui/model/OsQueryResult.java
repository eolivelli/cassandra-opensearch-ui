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
 * Result of an arbitrary OpenSearch REST call run from the query page. Unlike the browsing
 * endpoints, a non-2xx response is not treated as an error here: the HTTP status and the response
 * body (which is usually OpenSearch's own error JSON) are returned so the user can inspect them.
 *
 * @param request
 *            the REST call that was made
 * @param status
 *            the HTTP status code returned by OpenSearch
 * @param response
 *            the parsed response body (a JSON text node when the body was not valid JSON)
 */
public record OsQueryResult(OsRequest request, int status, JsonNode response) {
}
