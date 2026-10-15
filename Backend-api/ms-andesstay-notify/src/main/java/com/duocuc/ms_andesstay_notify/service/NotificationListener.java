package com.duocuc.ms_andesstay_notify.service;

import com.duocuc.ms_andesstay_notify.config.NotificationSimulationProperties;
import com.duocuc.ms_andesstay_notify.messaging.AbstractDlqListener;
import com.duocuc.ms_andesstay_notify.messaging.exception.NonRecoverableException;
import com.duocuc.ms_andesstay_notify.messaging.exception.RecoverableException;
import com.duocuc.ms_andesstay_notify.model.NotificationEvent;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Consumidor de notificaciones: una cola por canal (EMAIL, SMS, PUSH).
 *
 * <p>Cada método aplica el contrato de ACK manual heredado de
 * {@link AbstractDlqListener}: confirma con {@code basicAck} cuando el envío se
 * completa y descarta a la DLQ ({@code basicNack requeue=false}) cuando el
 * mensaje es inválido o se agotan los reintentos.</p>
 *
 * <p>La validación defensiva del payload vive aquí a propósito: un mensaje puede
 * llegar malformado desde cualquier productor (no sólo desde la API de notify),
 * y en ese caso debe terminar en la DLQ con log de ERROR en vez de provocar
 * reentregas infinitas.</p>
 */
@Service
public class NotificationListener extends AbstractDlqListener<NotificationEvent> {

    private final NotificationSimulationProperties simulation;

    public NotificationListener(RetryTemplate messagingRetryTemplate,
                                NotificationSimulationProperties simulation) {
        super(messagingRetryTemplate);
        this.simulation = simulation;
    }

    @RabbitListener(queues = "${andesstay.messaging.queues.notify-email}")
    public void onEmail(NotificationEvent event, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                        @Header(AmqpHeaders.REDELIVERED) boolean redelivered) throws IOException {
        handle("EMAIL", event, channel, deliveryTag, redelivered);
    }

    @RabbitListener(queues = "${andesstay.messaging.queues.notify-sms}")
    public void onSms(NotificationEvent event, Channel channel,
                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                      @Header(AmqpHeaders.REDELIVERED) boolean redelivered) throws IOException {
        handle("SMS", event, channel, deliveryTag, redelivered);
    }

    @RabbitListener(queues = "${andesstay.messaging.queues.notify-push}")
    public void onPush(NotificationEvent event, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                       @Header(AmqpHeaders.REDELIVERED) boolean redelivered) throws IOException {
        handle("PUSH", event, channel, deliveryTag, redelivered);
    }

    /**
     * Envío de la notificación por el canal que corresponde a la cola.
     *
     * <p>No tiene efectos persistentes (registra el envío simulado), por lo que es
     * idempotente por construcción: una reentrega repetiría el mismo log. Si en
     * el futuro se llama a un proveedor real, habrá que deduplicar por
     * {@code notificationId}.</p>
     */
    @Override
    protected void process(String channelName, NotificationEvent event) {
        requireValid(channelName, event);

        if (simulation.isFailing(event.getRecipient())) {
            throw new RecoverableException("Fallo transitorio simulado del proveedor " + channelName
                    + " para " + event.getRecipient());
        }

        switch (channelName) {
            case "EMAIL" -> log.info("[EMAIL] Enviado a: {} para la reserva #{}. Contenido: {}",
                    event.getRecipient(), event.getReservationId(), event.getBody());
            case "SMS" -> log.info("[SMS] Enviado a: {}. Mensaje: {}",
                    event.getRecipient(), event.getBody());
            case "PUSH" -> log.info("[PUSH] Enviada alerta a: {}. Notificación: {}",
                    event.getRecipient(), event.getBody());
            default -> throw new NonRecoverableException("Canal no soportado: " + channelName);
        }
    }

    /**
     * Validación defensiva: un payload incompleto o incoherente es un error
     * <b>no recuperable</b> (reintentarlo daría siempre el mismo resultado).
     */
    private void requireValid(String channelName, NotificationEvent event) {
        if (event == null) {
            throw new NonRecoverableException("Payload nulo en " + channelName);
        }
        if (isBlank(event.getRecipient())) {
            throw new NonRecoverableException("Payload sin destinatario (recipient) en " + channelName);
        }
        if (isBlank(event.getBody())) {
            throw new NonRecoverableException("Payload sin contenido (body) en " + channelName);
        }
        if (isBlank(event.getChannel()) || !channelName.equalsIgnoreCase(event.getChannel().trim())) {
            throw new NonRecoverableException("Canal del mensaje ('" + event.getChannel()
                    + "') incoherente con la cola " + channelName);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
