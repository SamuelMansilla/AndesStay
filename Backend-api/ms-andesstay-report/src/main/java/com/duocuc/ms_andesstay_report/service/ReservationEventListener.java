
package com.duocuc.ms_andesstay_report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ReservationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventListener.class);
    private final ReportService reportService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReservationEventListener(ReportService reportService) {
        this.reportService = reportService;
    }

    @KafkaListener(
            topics = "reservations.events",
            groupId = "andesstay-report-group",
            autoStartup = "${kafka.enabled:false}"
    )
    public void consumeReservationEvent(String message) {
        log.info("📊 [KAFKA REPORT] Evento de reserva recibido: {}", message);
        try {
            JsonNode root = objectMapper.readTree(message);

            Long reservationId = root.hasNonNull("reservationId")
                    ? root.get("reservationId").asLong()
                    : (root.hasNonNull("id") ? root.get("id").asLong() : null);

            Long unitId = root.hasNonNull("unitId") ? root.get("unitId").asLong() : 1L;

            String status = root.hasNonNull("status")
                    ? root.get("status").asText()
                    : (root.hasNonNull("eventType") ? root.get("eventType").asText() : "CREADA");

            LocalDateTime timestamp = LocalDateTime.now();
            if (root.hasNonNull("timestamp")) {
                try {
                    timestamp = LocalDateTime.parse(root.get("timestamp").asText());
                } catch (Exception ignored) {}
            }

            String eventType = root.hasNonNull("eventType")
                    ? root.get("eventType").asText()
                    : (root.hasNonNull("type") ? root.get("type").asText() : null);

            LocalDateTime createdAt = null;
            if (root.hasNonNull("createdAt")) {
                try {
                    createdAt = LocalDateTime.parse(root.get("createdAt").asText());
                } catch (Exception ignored) {}
            }

            if (reservationId != null) {
                reportService.recordReservationEvent(reservationId, unitId, status, eventType, timestamp, createdAt);
                log.info("✅ [KAFKA REPORT] Métrica persistida con éxito para reserva #{}", reservationId);
            }
        } catch (Exception e) {
            log.error("⚠️ [KAFKA REPORT] Error al procesar mensaje de streaming: {}", e.getMessage(), e);
        }
    }
}