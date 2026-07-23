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
package com.dbui.store.web;

import com.datastax.oss.driver.api.core.AllNodesFailedException;
import com.datastax.oss.driver.api.core.DriverException;
import com.dbui.store.auth.NotAuthenticatedException;
import com.dbui.store.opensearch.OpenSearchClient;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Translates backend failures (database down, bad input, missing resource, not signed in) into
 * clean JSON error responses so the UI can show a helpful message instead of a stack trace. Applied
 * to REST controllers only; SPA routes are handled separately.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NotAuthenticatedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthenticated(NotAuthenticatedException ex) {
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage(), ex);
    }

    @ExceptionHandler({NotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(Exception ex) {
        String message = ex instanceof NoResourceFoundException
                ? "No such resource"
                : ex.getMessage();
        return error(HttpStatus.NOT_FOUND, message, ex);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex) {
        String message;
        if (ex instanceof MethodArgumentNotValidException manv) {
            message = firstValidationMessage(manv);
        } else if (ex instanceof HttpMessageNotReadableException) {
            message = "Malformed or missing request body";
        } else {
            message = ex.getMessage();
        }
        return error(HttpStatus.BAD_REQUEST, message, ex);
    }

    @ExceptionHandler(AllNodesFailedException.class)
    public ResponseEntity<Map<String, Object>> handleCassandraDown(AllNodesFailedException ex) {
        log.warn("Cassandra unavailable: {}", ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE,
                "Cannot reach Cassandra. Is it running? (docker compose up)", ex);
    }

    @ExceptionHandler(OpenSearchClient.OpenSearchException.class)
    public ResponseEntity<Map<String, Object>> handleOpenSearch(
            OpenSearchClient.OpenSearchException ex) {
        log.warn("OpenSearch error: {}", ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
    }

    @ExceptionHandler(DriverException.class)
    public ResponseEntity<Map<String, Object>> handleCassandra(DriverException ex) {
        log.warn("Cassandra error: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "Cassandra error: " + ex.getMessage(), ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        log.error("Unexpected error", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
    }

    private String firstValidationMessage(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream().findFirst()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage()).orElse("Invalid request");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message,
            Exception ex) {
        Map<String, Object> body = Map.of("timestamp", Instant.now().toString(), "status",
                status.value(), "error", status.getReasonPhrase(), "message",
                message == null ? ex.getClass().getSimpleName() : message);
        return ResponseEntity.status(status).body(body);
    }
}
