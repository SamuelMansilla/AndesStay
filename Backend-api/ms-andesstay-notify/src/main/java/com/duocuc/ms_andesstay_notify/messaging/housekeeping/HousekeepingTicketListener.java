package com.duocuc.ms_andesstay_notify.messaging.housekeeping;

import com.duocuc.ms_andesstay_notify.messaging.AbstractDlqListener;
import com.duocuc.ms_andesstay_notify.model.TicketEvent;
import com.duocuc.ms_andesstay_notify.service.HousekeepingService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Consumidor del dominio <b>HOUSEKEEPING</b> — tickets de aseo.
 *
 * <ul>
 *   <li><b>Cola:</b> {@code q.housekeeping.tickets}</li>
 *   <li><b>Routing key:</b> {@code housekeeping.ticket}</li>
 *   <li><b>ACK:</b> manual ({@link AbstractDlqListener}): {@code basicAck} al
 *       registrar el ticket y descarte a la DLQ si el payload es inválido o se
 *       agotan los reintentos (tarea 05).</li>
 *   <li><b>Productor:</b> {@code reservations} al pasar la reserva a CHECKOUT
 *       (tarea 07).</li>
 * </ul>
 */
@Service
public class HousekeepingTicketListener extends AbstractDlqListener<TicketEvent> {

    private final HousekeepingService housekeepingService;

    public HousekeepingTicketListener(RetryTemplate messagingRetryTemplate,
                                      HousekeepingService housekeepingService) {
        super(messagingRetryTemplate);
        this.housekeepingService = housekeepingService;
    }

    @RabbitListener(queues = "${andesstay.messaging.queues.housekeeping-tickets}")
    public void onTicket(TicketEvent ticket, Channel channel,
                         @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                         @Header(AmqpHeaders.REDELIVERED) boolean redelivered) throws IOException {
        handle("HOUSEKEEPING", ticket, channel, deliveryTag, redelivered);
    }

    @Override
    protected void process(TicketEvent ticket) {
        housekeepingService.createTicket(ticket);
    }
}
