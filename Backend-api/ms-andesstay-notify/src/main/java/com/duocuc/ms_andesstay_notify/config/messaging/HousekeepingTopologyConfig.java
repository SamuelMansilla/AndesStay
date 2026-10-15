package com.duocuc.ms_andesstay_notify.config.messaging;

import com.duocuc.ms_andesstay_notify.config.MessagingProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dominio <b>HOUSEKEEPING</b> (caso de uso: ticket de aseo por reserva).
 *
 * <p>Ruta declarada (con dead-lettering hacia el DLX):</p>
 * <pre>
 *   ex.andesstay.notifications  --[housekeeping.ticket]--&gt;  q.housekeeping.tickets
 * </pre>
 *
 * <p>Productor: {@code reservations} al pasar la reserva a CHECKOUT (tarea 07).
 * Consumidor: {@code notify} (tarea 06).</p>
 */
@Configuration
public class HousekeepingTopologyConfig {

    private final MessagingProperties messaging;

    public HousekeepingTopologyConfig(MessagingProperties messaging) {
        this.messaging = messaging;
    }

    /** Cola de tickets de aseo que genera el checkout de una reserva. */
    @Bean
    public Queue housekeepingTicketsQueue() {
        return TopologySupport.durableWithDeadLetter(messaging.getQueues().getHousekeepingTickets(), messaging);
    }

    /** Ruta: ex.andesstay.notifications --[housekeeping.ticket]--> q.housekeeping.tickets */
    @Bean
    public Binding housekeepingTicketsBinding(Queue housekeepingTicketsQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(housekeepingTicketsQueue).to(notificationsExchange)
                .with(messaging.getRoutingKeys().getHousekeepingTicket());
    }
}
