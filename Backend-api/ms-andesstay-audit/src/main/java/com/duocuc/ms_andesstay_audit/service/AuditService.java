package com.duocuc.ms_andesstay_audit.service;

import com.duocuc.ms_andesstay_audit.model.AuditLog;
import com.duocuc.ms_andesstay_audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // Escucha eventos en reservations.events y audit.timeline según la topología del Caso 5
    @KafkaListener(
            topics = {"reservations.events", "audit.timeline"},
            groupId = "audit-group",
            autoStartup = "${kafka.enabled:false}"
    )
    public void consumeAuditMessage(String message) {
        log.info("📋 [KAFKA AUDIT] Evento recibido: {}", message);
        try {
            JsonNode root = objectMapper.readTree(message);

            String eventType = root.hasNonNull("eventType")
                    ? root.get("eventType").asText()
                    : (root.hasNonNull("type") ? root.get("type").asText() : "EVENTO_RESERVA");

            String actor = root.hasNonNull("actor")
                    ? root.get("actor").asText()
                    : (root.hasNonNull("guestEmail") ? root.get("guestEmail").asText() : "Sistema");

            Long reservationId = root.hasNonNull("reservationId")
                    ? root.get("reservationId").asLong()
                    : (root.hasNonNull("id") ? root.get("id").asLong() : null);

            LocalDateTime timestamp = LocalDateTime.now();
            if (root.hasNonNull("timestamp")) {
                try {
                    timestamp = LocalDateTime.parse(root.get("timestamp").asText());
                } catch (Exception ignored) {}
            }

            String details = root.hasNonNull("details")
                    ? root.get("details").asText()
                    : ("Evento " + eventType + " en reserva #" + (reservationId != null ? reservationId : "N/A"));

            AuditLog auditLog = new AuditLog(eventType, actor, reservationId, timestamp, details, message);
            auditLogRepository.save(auditLog);
            log.info("✅ [KAFKA AUDIT] Registro de auditoría persistido: ID #{}, evento {}", auditLog.getId(), eventType);
        } catch (Exception e) {
            log.warn("⚠️ [KAFKA AUDIT] Mensaje recibido en texto plano o formato simple: {}", e.getMessage());
            AuditLog fallbackLog = new AuditLog(
                    "EVENTO_KAFKA",
                    "Sistema",
                    null,
                    LocalDateTime.now(),
                    message,
                    message
            );
            auditLogRepository.save(fallbackLog);
        }
    }

    public AuditLog recordLog(String eventType, String actor, Long reservationId, String details, String payload) {
        AuditLog auditLog = new AuditLog(
                eventType,
                actor != null && !actor.isBlank() ? actor : "Sistema",
                reservationId,
                LocalDateTime.now(),
                details != null ? details : eventType,
                payload != null ? payload : details
        );
        return auditLogRepository.save(auditLog);
    }

    // Métodos de solo lectura para la API
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    public List<AuditLog> getLogsFiltered(String actor, String eventType, Long reservationId) {
        if ((actor == null || actor.isBlank()) &&
            (eventType == null || eventType.isBlank() || "TODOS".equalsIgnoreCase(eventType)) &&
            reservationId == null) {
            return auditLogRepository.findAllByOrderByTimestampDesc();
        }
        String cleanType = ("TODOS".equalsIgnoreCase(eventType)) ? null : eventType;
        String cleanActor = (actor != null && !actor.isBlank()) ? actor.trim() : null;
        return auditLogRepository.searchLogs(cleanActor, cleanType, reservationId);
    }

    public List<AuditLog> getLogsByActor(String actor) {
        return auditLogRepository.findByActorIgnoreCaseOrderByTimestampDesc(actor);
    }
}