package cl.duoc.andesstay.ms_andesstay_reservations.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ReservationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventPublisher.class);

    private final Optional<KafkaTemplate<String, String>> kafkaTemplate;

    @Value("${kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Value("${services.report.url:http://localhost:8084}")
    private String reportServiceUrl;

    @Value("${services.audit.url:http://localhost:8085}")
    private String auditServiceUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(800))
            .build();

    public ReservationEventPublisher(@Autowired(required = false) KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = Optional.ofNullable(kafkaTemplate);
    }

    /**
     * Publica eventos de reserva con envelope estándar según Caso 5:
     * - Topología Kafka: Tópicos 'reservations.events' y 'audit.timeline'
     * - Regla clave: Streaming sin bloquear el core
     */
    public void publishReservationEvent(
            Long reservationId,
            Long unitId,
            String status,
            String actor,
            String eventType,
            String details) {
        publishReservationEvent(reservationId, unitId, status, actor, eventType, details, LocalDateTime.now());
    }

    public void publishReservationEvent(
            Long reservationId,
            Long unitId,
            String status,
            String actor,
            String eventType,
            String details,
            LocalDateTime createdAt) {

        String eventId = UUID.randomUUID().toString();
        String traceId = "trace-" + eventId.substring(0, 8);
        String correlationId = "corr-" + eventId.substring(0, 8);
        String timestamp = LocalDateTime.now().toString();
        String createdStr = (createdAt != null ? createdAt : LocalDateTime.now()).toString();

        String payloadJson = String.format(
                "{\"eventId\":\"%s\",\"eventType\":\"%s\",\"type\":\"%s\",\"timestamp\":\"%s\"," +
                "\"createdAt\":\"%s\",\"traceId\":\"%s\",\"correlationId\":\"%s\",\"actor\":\"%s\"," +
                "\"reservationId\":%d,\"unitId\":%d,\"status\":\"%s\",\"details\":\"%s\"}",
                eventId, eventType, eventType, timestamp,
                createdStr, traceId, correlationId, actor != null ? actor : "Sistema",
                reservationId, unitId != null ? unitId : 1L, status, details
        );

        // 1. Streaming Kafka (si está habilitado)
        if (kafkaEnabled && kafkaTemplate.isPresent()) {
            try {
                String key = String.valueOf(reservationId);
                kafkaTemplate.get().send("reservations.events", key, payloadJson)
                        .whenComplete((res, ex) -> {
                            if (ex != null) {
                                log.warn("⚠️ [KAFKA] No se pudo enviar a reservations.events: {}", ex.getMessage());
                            } else {
                                log.info("🚀 [KAFKA] Evento publicado en reservations.events para reserva #{}", reservationId);
                            }
                        });

                kafkaTemplate.get().send("audit.timeline", key, payloadJson)
                        .whenComplete((res, ex) -> {
                            if (ex != null) {
                                log.warn("⚠️ [KAFKA] No se pudo enviar a audit.timeline: {}", ex.getMessage());
                            } else {
                                log.info("🚀 [KAFKA] Evento publicado en audit.timeline para reserva #{}", reservationId);
                            }
                        });
            } catch (Exception e) {
                log.warn("⚠️ [KAFKA] Fallo al intentar enviar mensaje a broker (no bloquea el core): {}", e.getMessage());
            }
        } else {
            log.info("ℹ️ [KAFKA] Kafka streaming deshabilitado o no configurado localmente. Ejecutando sincronización de soporte...");
        }

        // 2. Sincronización asíncrona de soporte para ambiente de desarrollo local sin clúster Kafka
        if (!kafkaEnabled || !kafkaTemplate.isPresent()) {
            CompletableFuture.runAsync(() -> notifyLocalServices(reservationId, unitId, status, actor, eventType, details, createdAt, payloadJson));
        }
    }

    private void notifyLocalServices(
            Long reservationId,
            Long unitId,
            String status,
            String actor,
            String eventType,
            String details,
            LocalDateTime createdAt,
            String payloadJson) {
        try {
            // Notificar a reportería (actualizar métricas inmediatas)
            String createdStr = (createdAt != null ? createdAt : LocalDateTime.now()).toString();
            String reportUrl = String.format("%s/api/report/events/simulate?reservationId=%d&unitId=%d&status=%s&eventType=%s&createdAt=%s",
                    reportServiceUrl,
                    reservationId,
                    unitId != null ? unitId : 1L,
                    status,
                    eventType != null ? eventType : "",
                    createdStr);
            HttpRequest reportReq = HttpRequest.newBuilder()
                    .uri(URI.create(reportUrl))
                    .timeout(Duration.ofMillis(1200))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            httpClient.sendAsync(reportReq, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(ex -> null);

            // Notificar a auditoría (persistir log en timeline inmediato)
            String auditUrl = String.format("%s/api/audit?eventType=%s&actor=%s&reservationId=%d&details=%s",
                    auditServiceUrl,
                    java.net.URLEncoder.encode(eventType != null ? eventType : "", java.nio.charset.StandardCharsets.UTF_8),
                    java.net.URLEncoder.encode(actor != null ? actor : "Sistema", java.nio.charset.StandardCharsets.UTF_8),
                    reservationId,
                    java.net.URLEncoder.encode(details != null ? details : "", java.nio.charset.StandardCharsets.UTF_8)
            );
            HttpRequest auditReq = HttpRequest.newBuilder()
                    .uri(URI.create(auditUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(1200))
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                    .build();
            httpClient.sendAsync(auditReq, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(ex -> null);

        } catch (Exception ignored) {
            // Regla clave: Nunca bloquear el core
        }
    }
}
