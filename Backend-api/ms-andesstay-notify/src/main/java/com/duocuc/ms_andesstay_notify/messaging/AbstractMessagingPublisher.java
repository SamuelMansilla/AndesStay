package com.duocuc.ms_andesstay_notify.messaging;

import com.duocuc.ms_andesstay_notify.config.MessagingProperties;
import org.springframework.amqp.core.AmqpTemplate;

/**
 * Base de los publishers de dominio.
 *
 * <p>Concentra el detalle de RabbitMQ (el {@link AmqpTemplate} y el exchange
 * principal) para que las clases de dominio expongan métodos de alto nivel.
 * Los nombres de exchange y routing keys salen siempre de
 * {@link MessagingProperties}, nunca de literales.</p>
 */
public abstract class AbstractMessagingPublisher {

    /** Template configurado con el converter JSON. */
    protected final AmqpTemplate amqpTemplate;

    /** Topología (exchange, routing keys) leída de application.yml. */
    protected final MessagingProperties messaging;

    protected AbstractMessagingPublisher(AmqpTemplate amqpTemplate, MessagingProperties messaging) {
        this.amqpTemplate = amqpTemplate;
        this.messaging = messaging;
    }

    /**
     * Publica un evento en el exchange principal con la routing key indicada.
     *
     * @param routingKey routing key del caso de uso (desde configuración)
     * @param payload    evento a publicar (se serializa a JSON)
     */
    protected void publish(String routingKey, Object payload) {
        amqpTemplate.convertAndSend(messaging.getExchange(), routingKey, payload);
    }
}
