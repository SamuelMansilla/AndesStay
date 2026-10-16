package com.duocuc.ms_andesstay_notify.messaging.notifications;

import com.duocuc.ms_andesstay_notify.messaging.AbstractDlqListener;
import com.duocuc.ms_andesstay_notify.model.NotificationEvent;
import com.duocuc.ms_andesstay_notify.service.NotificationService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Consumidor del dominio <b>NOTIFICATIONS</b> — canal <b>SMS</b>.
 *
 * <ul>
 *   <li><b>Cola:</b> {@code q.notify.sms}</li>
 *   <li><b>Routing key:</b> {@code notify.sms}</li>
 *   <li><b>ACK:</b> manual ({@link AbstractDlqListener}): {@code basicAck} al
 *       enviar y descarte a la DLQ si el payload es inválido o se agotan los
 *       reintentos (tarea 05).</li>
 *   <li><b>Productores:</b> {@code notify} (API).</li>
 * </ul>
 */
@Service
public class SmsListener extends AbstractDlqListener<NotificationEvent> {

    private final NotificationService notificationService;

    public SmsListener(RetryTemplate messagingRetryTemplate, NotificationService notificationService) {
        super(messagingRetryTemplate);
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${andesstay.messaging.queues.notify-sms}")
    public void onSms(NotificationEvent event, Channel channel,
                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                      @Header(AmqpHeaders.REDELIVERED) boolean redelivered) throws IOException {
        handle("SMS", event, channel, deliveryTag, redelivered);
    }

    @Override
    protected void process(NotificationEvent event) {
        notificationService.sendSms(event);
    }
}
