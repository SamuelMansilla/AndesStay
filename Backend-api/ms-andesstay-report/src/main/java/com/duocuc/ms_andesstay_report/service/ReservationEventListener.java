
/** package com.duocuc.ms_andesstay_report.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReservationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventListener.class);
    private final ReportService reportService;

    public ReservationEventListener(ReportService reportService) {
        this.reportService = reportService;
    }

    @KafkaListener(topics = "reservations.events", groupId = "andesstay-report-group", autoStartup = "${kafka.enabled:false}")
    public void consumeReservationEvent(String message) {
        log.info("Evento de reserva recibido vía Kafka para reportería: {}", message);
        // Procesa el payload JSON recibido para actualizar agregaciones
    }
}
*/