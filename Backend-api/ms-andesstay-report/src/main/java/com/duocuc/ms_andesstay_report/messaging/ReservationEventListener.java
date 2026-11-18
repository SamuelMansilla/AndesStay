package com.duocuc.ms_andesstay_report.messaging;

import com.duocuc.ms_andesstay_report.config.AndesStayKafkaProperties;
import com.duocuc.ms_andesstay_report.dto.ReservationEventDTO;
import com.duocuc.ms_andesstay_report.service.ReservationEventProcessor;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumidor de {@code reservations.events} (indicador 5 de la EP5).
 *
 * <p>Es un adaptador delgado: recibe el DTO validado y delega en
 * {@link ReservationEventProcessor}. No contiene lógica de negocio ni acceso a
 * la base de datos.</p>
 */
@Component
public class ReservationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventListener.class);

    private final ReservationEventProcessor processor;
    private final AndesStayKafkaProperties kafkaProperties;

    public ReservationEventListener(ReservationEventProcessor processor,
                                    AndesStayKafkaProperties kafkaProperties) {
        this.processor = processor;
        this.kafkaProperties = kafkaProperties;
    }

    @KafkaListener(
            topics = "${andesstay.kafka.topics.reservations-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory",
            autoStartup = "${kafka.enabled:false}",
            concurrency = "${andesstay.kafka.consumer.concurrency:3}")
    public void onReservationEvent(
            @Valid ReservationEventDTO event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key) {

        log.info("📥 [KAFKA REPORT] Evento recibido: eventId={}, reserva=#{}, status={}, key={}, topic={}, partition={}, offset={}",
                event.eventId(), event.reservationId(), event.status(), key, topic, partition, offset);

        simulateProcessingDelay();

        processor.process(event, topic, partition, offset);
    }

    /**
     * Retardo artificial para la demo de consumer lag (unidad 3, guía 3.3.2).
     * Por defecto 0 ms; se activa con {@code andesstay.kafka.consumer.simulated-delay-ms}.
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
            log.warn("⚠️ [KAFKA REPORT] Retardo de simulación interrumpido");
        }
    }
}
