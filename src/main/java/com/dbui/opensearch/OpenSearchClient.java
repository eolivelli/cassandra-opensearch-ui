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
import java.util.Locale;
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

    /**
     * Performs an arbitrary REST call for the query page. Unlike {@link #get}/{@link #post}, a
     * non-2xx response is returned rather than thrown, so the caller can surface OpenSearch's own
     * status and error body to the user. A blank body sends no request body.
     */
    public ClientResponse request(String method, String path, String jsonBody) {
        String verb = (method == null || method.isBlank())
                ? "GET"
                : method.strip().toUpperCase(Locale.ROOT);
        boolean hasBody = jsonBody != null && !jsonBody.isBlank();
        HttpRequest.BodyPublisher publisher = hasBody
                ? HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8)
                : HttpRequest.BodyPublishers.noBody();
        HttpRequest.Builder builder = baseRequest(path).method(verb, publisher);
        if (hasBody) {
            builder.header("Content-Type", "application/json");
        }
        return sendRaw(builder.build());
    }

    private HttpRequest.Builder baseRequest(String path) {
        String url = path.startsWith("http://") || path.startsWith("https://")
                ? path
                : properties.getUrl() + path;
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url))
                .timeout(Duration.ofSeconds(30)).header("Accept", "application/json");
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

    /**
     * Sends a request and returns the status and (leniently parsed) body without throwing on
     * 4xx/5xx.
     */
    private ClientResponse sendRaw(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = response.body();
            JsonNode node;
            if (body == null || body.isBlank()) {
                node = mapper.createObjectNode();
            } else {
                try {
                    node = mapper.readTree(body);
                } catch (IOException notJson) {
                    // e.g. a _cat text response — expose it as-is rather than failing.
                    node = mapper.getNodeFactory().textNode(body);
                }
            }
            return new ClientResponse(response.statusCode(), node);
        } catch (IOException e) {
            throw new OpenSearchException("Failed to call OpenSearch: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpenSearchException("Interrupted while calling OpenSearch", e);
        }
    }

    /** An HTTP status code paired with the parsed response body. */
    public record ClientResponse(int status, JsonNode body) {
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
