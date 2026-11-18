package com.duocuc.ms_andesstay_audit.service;

import com.duocuc.ms_andesstay_audit.dto.ReservationEventDTO;
import com.duocuc.ms_andesstay_audit.model.AuditLog;
import com.duocuc.ms_andesstay_audit.repository.AuditLogRepository;
import com.duocuc.ms_andesstay_audit.repository.ProcessedMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lógica de dominio del timeline de auditoría.
 *
 * <p>El {@code @KafkaListener} vive en {@code messaging/AuditEventListener};
 * este servicio sólo persiste eventos ya deserializados y validados.</p>
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private static final int EVENT_TYPE_MAX = 255;
    private static final int ACTOR_MAX = 255;
    private static final int DETAILS_MAX = 500;
    private static final int PAYLOAD_MAX = 2000;

    private final AuditLogRepository auditLogRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository,
                        ProcessedMessageRepository processedMessageRepository,
                        ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.processedMessageRepository = processedMessageRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Persiste un evento de auditoría de forma idempotente: si el {@code eventId}
     * ya fue procesado, no se inserta un segundo registro (unidad 3, guía 3.3.3).
     */
    @Transactional
    public void recordEvent(ReservationEventDTO event, String topic, int partition, long offset) {
        int inserted = processedMessageRepository.markProcessed(
                event.eventId(), topic, partition, offset);

        if (inserted == 0) {
            log.info("♻️ [KAFKA AUDIT] Evento duplicado ignorado: eventId={}, topic={}, partition={}, offset={}",
                    event.eventId(), topic, partition, offset);
            return;
        }

        String eventType = truncate(
                event.eventType() != null && !event.eventType().isBlank() ? event.eventType() : "EVENTO_RESERVA",
                EVENT_TYPE_MAX);
        String actor = truncate(
                event.actor() != null && !event.actor().isBlank() ? event.actor() : "Sistema",
                ACTOR_MAX);
        String details = truncate(
                event.details() != null && !event.details().isBlank()
                        ? event.details()
                        : "Evento " + eventType + " en reserva #" + event.reservationId(),
                DETAILS_MAX);
        String payload = truncate(toJson(event), PAYLOAD_MAX);

        AuditLog auditLog = new AuditLog(
                eventType,
                actor,
                event.reservationId(),
                parseTimestamp(event.timestamp()),
                details,
                payload);

        auditLogRepository.save(auditLog);
        log.info("✅ [KAFKA AUDIT] Registro persistido: ID #{}, eventId={}, reserva=#{}",
                auditLog.getId(), event.eventId(), event.reservationId());
    }

    public AuditLog recordLog(String eventType, String actor, Long reservationId, String details, String payload) {
        AuditLog auditLog = new AuditLog(
                truncate(eventType, EVENT_TYPE_MAX),
                truncate(actor != null && !actor.isBlank() ? actor : "Sistema", ACTOR_MAX),
                reservationId,
                LocalDateTime.now(),
                truncate(details != null ? details : eventType, DETAILS_MAX),
                truncate(payload != null ? payload : details, PAYLOAD_MAX));
        return auditLogRepository.save(auditLog);
    }

    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    public List<AuditLog> getLogsFiltered(String actor, String eventType, Long reservationId) {
        String cleanType = ("TODOS".equalsIgnoreCase(eventType)) ? null : eventType;
        String cleanActor = (actor != null && !actor.isBlank()) ? actor.trim() : null;

        // Caminos rápidos: evitan la consulta dinámica cuando hay un solo filtro.
        if (cleanActor == null && cleanType == null && reservationId == null) {
            return auditLogRepository.findAllByOrderByTimestampDesc();
        }
        if (cleanActor == null && cleanType == null) {
            return auditLogRepository.findByReservationIdOrderByTimestampDesc(reservationId);
        }
        if (cleanActor == null && reservationId == null) {
            return auditLogRepository.findByEventTypeContainingIgnoreCaseOrderByTimestampDesc(cleanType);
        }
        if (cleanType == null && reservationId == null) {
            return auditLogRepository.findByActorContainingIgnoreCaseOrderByTimestampDesc(cleanActor);
        }
        return auditLogRepository.searchLogs(cleanActor, cleanType, reservationId);
    }

    public List<AuditLog> getLogsByActor(String actor) {
        return auditLogRepository.findByActorIgnoreCaseOrderByTimestampDesc(actor);
    }

    private LocalDateTime parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ex) {
            log.warn("⚠️ [KAFKA AUDIT] Timestamp no parseable '{}'; se usa ahora", value);
            return LocalDateTime.now();
        }
    }

    private String toJson(ReservationEventDTO event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            return String.valueOf(event);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
