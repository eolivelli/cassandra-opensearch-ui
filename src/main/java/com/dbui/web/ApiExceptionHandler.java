package com.dbui.web;

import com.dbui.opensearch.OpenSearchClient;
import com.datastax.oss.driver.api.core.AllNodesFailedException;
import com.datastax.oss.driver.api.core.DriverException;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates backend failures (database down, bad query) into clean JSON error
 * responses so the UI can show a helpful message instead of a stack trace.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({AllNodesFailedException.class})
    public ResponseEntity<Map<String, Object>> handleCassandraDown(AllNodesFailedException ex) {
        log.warn("Cassandra unavailable: {}", ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE,
                "Cannot reach Cassandra. Is it running? (docker compose up)", ex);
    }

    @ExceptionHandler(OpenSearchClient.OpenSearchException.class)
    public ResponseEntity<Map<String, Object>> handleOpenSearch(OpenSearchClient.OpenSearchException ex) {
        log.warn("OpenSearch error: {}", ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(IllegalArgumentException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
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

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message, Exception ex) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message == null ? ex.getClass().getSimpleName() : message);
        return ResponseEntity.status(status).body(body);
    }
}
