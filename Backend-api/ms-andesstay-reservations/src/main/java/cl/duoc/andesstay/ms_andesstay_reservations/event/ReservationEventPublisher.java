package cl.duoc.andesstay.ms_andesstay_reservations.event;

import cl.duoc.andesstay.ms_andesstay_reservations.config.AndesStayKafkaProperties;
import cl.duoc.andesstay.ms_andesstay_reservations.dto.ReservationEventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Publica eventos de reserva en los dos tópicos de la EP5.
 *
 * <p>Reglas:</p>
 * <ul>
 *   <li>El payload es un {@link ReservationEventDTO} (JSON tipado), no un String
 *       armado con {@code String.format}.</li>
 *   <li>La key es el {@code reservationId}: todos los eventos de una reserva van
 *       a la misma partición y conservan el orden.</li>
 *   <li>Los nombres de tópico salen de {@link AndesStayKafkaProperties}.</li>
 *   <li>La publicación es asíncrona y nunca bloquea el core; si Kafka no está
 *       habilitado, se conserva el fallback HTTP local de la EP3.</li>
 * </ul>
 */
@Component
public class ReservationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventPublisher.class);

    private static final String HEADER_EVENT_ID = "X-Event-Id";
    private static final String HEADER_TRACE_ID = "X-Trace-Id";
    private static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    private final Optional<KafkaTemplate<String, ReservationEventDTO>> kafkaTemplate;
    private final AndesStayKafkaProperties kafkaProperties;
    private final ObjectMapper objectMapper;

    @Value("${kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Value("${services.report.url:http://localhost:8084}")
    private String reportServiceUrl;

    @Value("${services.audit.url:http://localhost:8085}")
    private String auditServiceUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(800))
            .build();

    public ReservationEventPublisher(
            @Autowired(required = false) KafkaTemplate<String, ReservationEventDTO> kafkaTemplate,
            AndesStayKafkaProperties kafkaProperties,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = Optional.ofNullable(kafkaTemplate);
        this.kafkaProperties = kafkaProperties;
        this.objectMapper = objectMapper;
    }

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

        ReservationEventDTO event = buildEvent(reservationId, unitId, status, actor, eventType, details, createdAt);

        if (kafkaEnabled && kafkaTemplate.isPresent()) {
            publishToTopic(kafkaProperties.getTopics().getReservationsEvents(), event);
            publishToTopic(kafkaProperties.getTopics().getAuditTimeline(), event);
        } else {
            log.info("ℹ️ [KAFKA] Streaming deshabilitado o sin template. Ejecutando sincronización HTTP de soporte...");
            CompletableFuture.runAsync(() -> notifyLocalServices(event));
        }
    }

    private ReservationEventDTO buildEvent(
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

        return new ReservationEventDTO(
                eventId,
                eventType,
                eventType,
                timestamp,
                createdStr,
                traceId,
                correlationId,
                actor != null ? actor : "Sistema",
                reservationId,
                unitId != null ? unitId : 1L,
                status,
                details);
    }

    private void publishToTopic(String topic, ReservationEventDTO event) {
        if (topic == null || topic.isBlank()) {
            log.warn("⚠️ [KAFKA] Tópico no configurado para el evento {}; se omite la publicación", event.eventId());
            return;
        }

        ProducerRecord<String, ReservationEventDTO> record =
                new ProducerRecord<>(topic, String.valueOf(event.reservationId()), event);
        record.headers().add(HEADER_EVENT_ID, event.eventId().getBytes(StandardCharsets.UTF_8));
        record.headers().add(HEADER_TRACE_ID, safe(event.traceId()).getBytes(StandardCharsets.UTF_8));
        record.headers().add(HEADER_CORRELATION_ID, safe(event.correlationId()).getBytes(StandardCharsets.UTF_8));

        try {
            kafkaTemplate.get().send(record).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.warn("⚠️ [KAFKA] No se pudo publicar en {} (eventId={}, reserva=#{}): {}",
                            topic, event.eventId(), event.reservationId(), ex.getMessage());
                    return;
                }
                log.info("🚀 [KAFKA] Evento publicado en {} (partición={}, offset={}, eventId={}, reserva=#{})",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.eventId(),
                        event.reservationId());
            });
        } catch (Exception ex) {
            // Regla clave: nunca bloquear el core por un fallo de mensajería.
            log.warn("⚠️ [KAFKA] Fallo al encolar el evento en {}: {}", topic, ex.getMessage());
        }
    }

    /**
     * Fallback HTTP para desarrollo local sin clúster Kafka (se mantiene de la EP3).
     */
    private void notifyLocalServices(ReservationEventDTO event) {
        try {
            String createdStr = event.createdAt();

            String reportUrl = String.format(
                    "%s/api/report/events/simulate?reservationId=%d&unitId=%d&status=%s&eventType=%s&createdAt=%s",
                    reportServiceUrl,
                    event.reservationId(),
                    event.unitId(),
                    event.status(),
                    event.eventType(),
                    createdStr);
            HttpRequest reportReq = HttpRequest.newBuilder()
                    .uri(URI.create(reportUrl))
                    .timeout(Duration.ofMillis(1200))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            httpClient.sendAsync(reportReq, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(ex -> null);

            String auditUrl = String.format(
                    "%s/api/audit?eventType=%s&actor=%s&reservationId=%d&details=%s",
                    auditServiceUrl,
                    URLEncoder.encode(safe(event.eventType()), StandardCharsets.UTF_8),
                    URLEncoder.encode(safe(event.actor()), StandardCharsets.UTF_8),
                    event.reservationId(),
                    URLEncoder.encode(safe(event.details()), StandardCharsets.UTF_8));
            HttpRequest auditReq = HttpRequest.newBuilder()
                    .uri(URI.create(auditUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(1200))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(event)))
                    .build();
            httpClient.sendAsync(auditReq, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(ex -> null);
        } catch (Exception ignored) {
            // Regla clave: nunca bloquear el core.
        }
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}
