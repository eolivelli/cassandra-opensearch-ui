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

/**
 * Body of a request to run an arbitrary OpenSearch REST call from the query page.
 *
 * @param method
 *            HTTP method (GET, POST, PUT, DELETE, HEAD); defaults to GET when blank
 * @param path
 *            request path or URI (relative to the configured base URL, e.g. {@code /index/_search})
 * @param body
 *            optional JSON request body
 */
public record OsQueryRequest(String method, String path, String body) {
}
