package com.dbui.model;

/**
 * A description of the OpenSearch REST call that produced a result, shown in the
 * UI so users can reproduce it (e.g. with curl).
 *
 * @param method HTTP method
 * @param path   request path including query string
 * @param body   request body, or {@code null} for bodyless requests
 */
public record OsRequest(String method, String path, String body) {

    public static OsRequest get(String path) {
        return new OsRequest("GET", path, null);
    }

    public static OsRequest post(String path, String body) {
        return new OsRequest("POST", path, body);
    }
}
