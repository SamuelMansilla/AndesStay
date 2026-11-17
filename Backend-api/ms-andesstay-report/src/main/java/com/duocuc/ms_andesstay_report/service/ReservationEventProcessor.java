package com.duocuc.ms_andesstay_report.service;

import com.duocuc.ms_andesstay_report.dto.ReservationEventDTO;
import com.duocuc.ms_andesstay_report.repository.ProcessedMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Procesa un evento de reserva ya deserializado y validado.
 *
 * <p>La idempotencia y la proyección de KPIs ocurren en la misma transacción:
 * si el {@code eventId} ya fue procesado, no se vuelve a aplicar el efecto de
 * negocio (unidad 3, guía 3.3.3).</p>
 */
@Service
public class ReservationEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventProcessor.class);

    private final ProcessedMessageRepository processedMessageRepository;
    private final ReportService reportService;

    public ReservationEventProcessor(ProcessedMessageRepository processedMessageRepository,
                                     ReportService reportService) {
        this.processedMessageRepository = processedMessageRepository;
        this.reportService = reportService;
    }

    @Transactional
    public void process(ReservationEventDTO event, String topic, int partition, long offset) {
        int inserted = processedMessageRepository.markProcessed(
                event.eventId(), topic, partition, offset);

        if (inserted == 0) {
            log.info("♻️ [KAFKA REPORT] Evento duplicado ignorado: eventId={}, topic={}, partition={}, offset={}",
                    event.eventId(), topic, partition, offset);
            return;
        }

        Long unitId = event.unitId() != null ? event.unitId() : 1L;
        reportService.recordReservationEvent(
                event.reservationId(),
                unitId,
                event.status(),
                event.eventType(),
                parseTimestamp(event.timestamp()),
                parseTimestamp(event.createdAt()));
    }

    private LocalDateTime parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ex) {
            log.warn("⚠️ [KAFKA REPORT] Timestamp no parseable '{}'; se usa ahora", value);
            return LocalDateTime.now();
        }
    }
}
