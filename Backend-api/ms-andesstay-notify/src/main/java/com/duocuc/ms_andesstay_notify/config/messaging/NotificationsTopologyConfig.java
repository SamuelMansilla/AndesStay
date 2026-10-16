package com.duocuc.ms_andesstay_notify.config.messaging;

import com.duocuc.ms_andesstay_notify.config.MessagingProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dominio <b>NOTIFICATIONS</b> (caso de uso: enviar notificaciones por canal).
 *
 * <p>Rutas declaradas (todas con dead-lettering hacia el DLX):</p>
 * <pre>
 *   ex.andesstay.notifications  --[notify.email]--&gt;  q.notify.email
 *   ex.andesstay.notifications  --[notify.sms]  --&gt;  q.notify.sms
 *   ex.andesstay.notifications  --[notify.push] --&gt;  q.notify.push
 * </pre>
 *
 * <p>Productores: {@code notify} (API REST) y {@code reservations} (confirmacion).
 * Consumidores: {@code notify} (listeners por canal, tarea 06).</p>
 */
@Configuration
public class NotificationsTopologyConfig {

    private final MessagingProperties messaging;

    public NotificationsTopologyConfig(MessagingProperties messaging) {
        this.messaging = messaging;
    }

    /** Exchange principal: punto de publicacion de todos los dominios. */
    @Bean
    public DirectExchange notificationsExchange() {
        return new DirectExchange(messaging.getExchange(), true, false);
    }

    // ── Caso de uso: notificacion por EMAIL ──────────────────────────────────

    /** Cola de notificaciones por email. */
    @Bean
    public Queue notifyEmailQueue() {
        return TopologySupport.durableWithDeadLetter(messaging.getQueues().getNotifyEmail(), messaging);
    }

    /** Ruta: ex.andesstay.notifications --[notify.email]--> q.notify.email */
    @Bean
    public Binding notifyEmailBinding(Queue notifyEmailQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(notifyEmailQueue).to(notificationsExchange)
                .with(messaging.getRoutingKeys().getNotifyEmail());
    }

    // ── Caso de uso: notificacion por SMS ────────────────────────────────────

    /** Cola de notificaciones por SMS. */
    @Bean
    public Queue notifySmsQueue() {
        return TopologySupport.durableWithDeadLetter(messaging.getQueues().getNotifySms(), messaging);
    }

    /** Ruta: ex.andesstay.notifications --[notify.sms]--> q.notify.sms */
    @Bean
    public Binding notifySmsBinding(Queue notifySmsQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(notifySmsQueue).to(notificationsExchange)
                .with(messaging.getRoutingKeys().getNotifySms());
    }

    // ── Caso de uso: notificacion PUSH ───────────────────────────────────────

    /** Cola de notificaciones push (alertas a la app). */
    @Bean
    public Queue notifyPushQueue() {
        return TopologySupport.durableWithDeadLetter(messaging.getQueues().getNotifyPush(), messaging);
    }

    /** Ruta: ex.andesstay.notifications --[notify.push]--> q.notify.push */
    @Bean
    public Binding notifyPushBinding(Queue notifyPushQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(notifyPushQueue).to(notificationsExchange)
                .with(messaging.getRoutingKeys().getNotifyPush());
    }
}
