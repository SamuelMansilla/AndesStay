package com.duocuc.ms_andesstay_notify.messaging.housekeeping;

import com.duocuc.ms_andesstay_notify.config.MessagingProperties;
import com.duocuc.ms_andesstay_notify.messaging.AbstractMessagingPublisher;
import com.duocuc.ms_andesstay_notify.model.TicketEvent;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;

/**
 * Dominio <b>HOUSEKEEPING</b>: publica tickets de aseo.
 *
 * <p>Ruta publicada: routing key {@code housekeeping.ticket} hacia
 * {@code q.housekeeping.tickets} (exchange principal). El productor principal es
 * {@code reservations} al pasar la reserva a CHECKOUT; este publisher permite
 * también emitirlos desde {@code notify}.</p>
 */
@Component
public class HousekeepingTicketPublisher extends AbstractMessagingPublisher {

    public HousekeepingTicketPublisher(AmqpTemplate amqpTemplate, MessagingProperties messaging) {
        super(amqpTemplate, messaging);
    }

    /** Publica un ticket de aseo para su procesamiento asíncrono. */
    public void publish(TicketEvent ticket) {
        publish(messaging.getRoutingKeys().getHousekeepingTicket(), ticket);
    }
}
