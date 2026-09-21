package cl.duoc.andesstay.ms_andesstay_bff.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class BffExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BffExceptionHandler.class);

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<Map<String, Object>> handleConnectionRefused(WebClientRequestException ex) {
        String uri = ex.getUri() != null ? ex.getUri().toString() : "microservicio interno";
        log.error("❌ [BFF -> MICROSERVICIO NO DISPONIBLE] No se pudo conectar a {}: {}", uri, ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 502,
                "error", "Bad Gateway",
                "message", "El microservicio de dominio en " + uri + " no está respondiendo. Verifica que esté corriendo en tu terminal."
        ));
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<String> handleDownstreamResponseError(WebClientResponseException ex) {
        log.error("❌ [BFF -> MICROSERVICIO ERROR] Status {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        MediaType contentType = ex.getHeaders().getContentType() != null 
                ? ex.getHeaders().getContentType() 
                : MediaType.APPLICATION_JSON;

        return ResponseEntity.status(ex.getStatusCode())
                .contentType(contentType)
                .body(ex.getResponseBodyAsString());
    }
}
