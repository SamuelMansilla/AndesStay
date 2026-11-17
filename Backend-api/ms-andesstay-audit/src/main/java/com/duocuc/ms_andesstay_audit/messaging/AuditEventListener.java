package com.duocuc.ms_andesstay_audit.messaging;

import com.duocuc.ms_andesstay_audit.config.AndesStayKafkaProperties;
import com.duocuc.ms_andesstay_audit.dto.ReservationEventDTO;
import com.duocuc.ms_andesstay_audit.service.AuditService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumidor de {@code audit.timeline} (indicador 5 de la EP5).
 *
 * <p>Adaptador delgado: valida el DTO y delega en {@link AuditService}.</p>
 */
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditService auditService;
    private final AndesStayKafkaProperties kafkaProperties;

    public AuditEventListener(AuditService auditService,
                              AndesStayKafkaProperties kafkaProperties) {
        this.auditService = auditService;
        this.kafkaProperties = kafkaProperties;
    }

    @KafkaListener(
            topics = "${andesstay.kafka.topics.audit-timeline}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory",
            autoStartup = "${kafka.enabled:false}",
            concurrency = "${andesstay.kafka.consumer.concurrency:3}")
    public void onAuditEvent(
            @Valid ReservationEventDTO event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key) {

        log.info("📥 [KAFKA AUDIT] Evento recibido: eventId={}, reserva=#{}, tipo={}, key={}, topic={}, partition={}, offset={}",
                event.eventId(), event.reservationId(), event.eventType(), key, topic, partition, offset);

        simulateProcessingDelay();

        auditService.recordEvent(event, topic, partition, offset);
    }

    /**
     * Retardo artificial para la demo de consumer lag (unidad 3, guía 3.3.2).
     */
    private void simulateProcessingDelay() {
        long delayMs = kafkaProperties.getConsumer().getSimulatedDelayMs();
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("⚠️ [KAFKA AUDIT] Retardo de simulación interrumpido");
        }
    }
}
