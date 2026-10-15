package com.duocuc.ms_andesstay_notify.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo global de errores de entrada de {@code notify}.
 *
 * <p>Traduce los rechazos de validación a respuestas <b>400</b> con mensajes
 * claros (indicador 8): un DTO inválido indica un payload que nunca debe llegar
 * a la cola, por lo que se rechaza antes de publicar.</p>
 */
@RestControllerAdvice
public class NotifyExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(NotifyExceptionHandler.class);

    /** DTO inválido: 400 con el detalle de cada campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fields.put(error.getField(), error.getDefaultMessage()));

        log.warn("[VALIDACION] Petición rechazada por campos inválidos: {}", fields);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 400);
        body.put("error", "Solicitud inválida");
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    /** Argumento inválido de negocio (p. ej. canal no soportado): 400. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[VALIDACION] Petición rechazada: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 400);
        body.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }
}
