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
 * A description of the OpenSearch REST call that produced a result, shown in the UI so users can
 * reproduce it (e.g. with curl).
 *
 * @param method
 *            HTTP method
 * @param path
 *            request path including query string
 * @param body
 *            request body, or {@code null} for bodyless requests
 */
public record OsRequest(String method, String path, String body) {

    public static OsRequest get(String path) {
        return new OsRequest("GET", path, null);
    }

    public static OsRequest post(String path, String body) {
        return new OsRequest("POST", path, body);
    }
}
