package com.duocuc.ms_andesstay_mqadmin.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de la API de administración a respuestas HTTP claras
 * (indicador 8 de la pauta).
 *
 * <table>
 *   <tr><th>Excepción</th><th>HTTP</th></tr>
 *   <tr><td>{@link MethodArgumentNotValidException} / {@link ConstraintViolationException}</td><td>400 con {@code fields}</td></tr>
 *   <tr><td>{@link ResourceNotFoundException}</td><td>404</td></tr>
 *   <tr><td>{@link IllegalArgumentException}</td><td>409</td></tr>
 *   <tr><td>{@link AmqpException}</td><td>502</td></tr>
 * </table>
 */
@RestControllerAdvice
public class MqAdminExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MqAdminExceptionHandler.class);

    /** Validación del cuerpo ({@code @Valid @RequestBody}). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleBodyValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return validationProblem(fields);
    }

    /** Validación de parámetros de ruta/consulta ({@code @PathVariable} con restricciones). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleParamValidation(ConstraintViolationException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            fields.putIfAbsent(field, violation.getMessage());
        });
        return validationProblem(fields);
    }

    /** Recurso no administrado. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message(ex.getMessage()));
    }

    /** Conflicto de recursos (por ejemplo, cola ya existente) o parámetro inválido de negocio. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(message(ex.getMessage()));
    }

    /** Fallo de comunicación con el broker. */
    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<Map<String, Object>> handleAmqp(AmqpException ex) {
        LOG.error("[MQADMIN] Fallo al comunicarse con el broker RabbitMQ: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(message("No se pudo contactar el broker RabbitMQ: " + ex.getMessage()));
    }

    private ResponseEntity<Map<String, Object>> validationProblem(Map<String, String> fields) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Datos de entrada inválidos");
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    private Map<String, Object> message(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        return body;
    }
}
