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
package com.dbui.opensearch;

import com.dbui.config.OpenSearchProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper over the OpenSearch HTTP REST API using the JDK {@link HttpClient}. Working at the
 * raw-REST level keeps the exact requests visible, which is what the UI surfaces to users.
 */
@Component
public class OpenSearchClient {

    private final OpenSearchProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient httpClient;

    public OpenSearchClient(OpenSearchProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /** Performs a GET and parses the JSON response body. */
    public JsonNode get(String path) {
        return send(baseRequest(path).GET().build());
    }

    /** Performs a POST with a JSON body and parses the JSON response. */
    public JsonNode post(String path, String jsonBody) {
        HttpRequest request = baseRequest(path).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
        return send(request);
    }

    private HttpRequest.Builder baseRequest(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(properties.getUrl() + path)).timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json");
        if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
            String token = properties.getUsername() + ":" + properties.getPassword();
            String encoded = Base64.getEncoder()
                    .encodeToString(token.getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + encoded);
        }
        return builder;
    }

    private JsonNode send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = response.body();
            if (response.statusCode() >= 400) {
                throw new OpenSearchException(
                        "OpenSearch returned HTTP " + response.statusCode() + ": " + body);
            }
            return body == null || body.isBlank()
                    ? mapper.createObjectNode()
                    : mapper.readTree(body);
        } catch (IOException e) {
            throw new OpenSearchException("Failed to call OpenSearch: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpenSearchException("Interrupted while calling OpenSearch", e);
        }
    }

    /** Raised when an OpenSearch request fails. */
    public static class OpenSearchException extends RuntimeException {
        public OpenSearchException(String message) {
            super(message);
        }

        public OpenSearchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
