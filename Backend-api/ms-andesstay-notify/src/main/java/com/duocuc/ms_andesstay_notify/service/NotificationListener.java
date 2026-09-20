package com.duocuc.ms_andesstay_notify.service;

import com.duocuc.ms_andesstay_notify.model.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    @RabbitListener(queues = "${andesstay.rabbitmq.queue.email}")
    public void processEmailNotification(NotificationEvent event) {
        log.info("[EMAIL] Enviado a: {} para la reserva #{}. Contenido: {}", 
                event.getRecipient(), event.getReservationId(), event.getBody());
    }

    @RabbitListener(queues = "${andesstay.rabbitmq.queue.sms}")
    public void processSmsNotification(NotificationEvent event) {
        log.info("[SMS] Enviado a: {}. Mensaje: {}", 
                event.getRecipient(), event.getBody());
    }

    @RabbitListener(queues = "${andesstay.rabbitmq.queue.push}")
    public void processPushNotification(NotificationEvent event) {
        log.info("[PUSH] Enviada alerta a: {}. Notificación: {}", 
                event.getRecipient(), event.getBody());
    }

    @RabbitListener(queues = "${andesstay.rabbitmq.queue.dlq}")
    public void processDeadLetter(NotificationEvent event) {
        log.warn("[DLQ] Evento en cola de fallos (Dead Letter Queue): {}", event);
    }
}