package com.duocuc.ms_andesstay_kafkaadmin.exception;

import jakarta.validation.ConstraintViolationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce errores de validación y de Kafka a HTTP con un body homogéneo.
 */
@RestControllerAdvice
public class KafkaAdminExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fields.put(error.getField(), error.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos", fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(ConstraintViolationException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                fields.put(violation.getPropertyPath().toString(), violation.getMessage()));
        return error(HttpStatus.BAD_REQUEST, "Parámetros inválidos", fields);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(TopicExistsException.class)
    public ResponseEntity<Map<String, Object>> handleTopicExists(TopicExistsException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler({InvalidReplicationFactorException.class, InvalidPartitionsException.class})
    public ResponseEntity<Map<String, Object>> handleInvalidTopicConfig(RuntimeException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(org.apache.kafka.common.errors.TimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleKafkaTimeout(
            org.apache.kafka.common.errors.TimeoutException ex) {
        return error(HttpStatus.GATEWAY_TIMEOUT, "El broker no respondió a tiempo: " + ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleKafkaUnavailable(IllegalStateException ex) {
        log.warn("⚠️ [KAFKAADMIN] Error de broker: {}", ex.getMessage());
        return error(HttpStatus.BAD_GATEWAY, ex.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("❌ [KAFKAADMIN] Error inesperado", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error inesperado: " + ex.getMessage(), null);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message,
                                                      Map<String, String> fields) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (fields != null && !fields.isEmpty()) {
            body.put("fields", fields);
        }
        return ResponseEntity.status(status).body(body);
    }
}
