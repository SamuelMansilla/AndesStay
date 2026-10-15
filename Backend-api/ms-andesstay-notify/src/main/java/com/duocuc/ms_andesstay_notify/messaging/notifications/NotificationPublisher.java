package com.duocuc.ms_andesstay_notify.messaging.notifications;

import com.duocuc.ms_andesstay_notify.config.MessagingProperties;
import com.duocuc.ms_andesstay_notify.messaging.AbstractMessagingPublisher;
import com.duocuc.ms_andesstay_notify.model.NotificationEvent;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;

/**
 * Dominio <b>NOTIFICATIONS</b>: publica notificaciones en la cola del canal
 * correspondiente.
 *
 * <table>
 *   <caption>Rutas publicadas</caption>
 *   <tr><th>Canal</th><th>Routing key</th><th>Destino</th></tr>
 *   <tr><td>EMAIL</td><td>notify.email</td><td>q.notify.email</td></tr>
 *   <tr><td>SMS</td><td>notify.sms</td><td>q.notify.sms</td></tr>
 *   <tr><td>PUSH</td><td>notify.push</td><td>q.notify.push</td></tr>
 * </table>
 */
@Component
public class NotificationPublisher extends AbstractMessagingPublisher {

    public NotificationPublisher(AmqpTemplate amqpTemplate, MessagingProperties messaging) {
        super(amqpTemplate, messaging);
    }

    /**
     * Publica una notificación en la cola del canal indicado por el evento.
     *
     * @param event notificación a enviar (canal EMAIL, SMS o PUSH)
     * @return la routing key utilizada
     * @throws IllegalArgumentException si el canal no es soportado
     */
    public String publish(NotificationEvent event) {
        String canal = event.getChannel() == null ? "" : event.getChannel().trim().toUpperCase();

        String routingKey = switch (canal) {
            case "EMAIL" -> messaging.getRoutingKeys().getNotifyEmail();
            case "SMS" -> messaging.getRoutingKeys().getNotifySms();
            case "PUSH" -> messaging.getRoutingKeys().getNotifyPush();
            default -> throw new IllegalArgumentException(
                    "Canal no soportado: '" + event.getChannel() + "'. Use EMAIL, SMS o PUSH.");
        };

        publish(routingKey, event);
        return routingKey;
    }
}
