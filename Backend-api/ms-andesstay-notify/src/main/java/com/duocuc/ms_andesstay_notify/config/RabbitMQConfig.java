package com.duocuc.ms_andesstay_notify.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans de infraestructura de mensajería (exchanges, colas, bindings, converter).
 *
 * <p>Ningún nombre de topología se escribe aquí: todos se leen de
 * {@link MessagingProperties}, que los toma de {@code application.yml}
 * (indicador 1 de la pauta).</p>
 *
 * <p>Las colas de los casos de uso nuevos (tickets de housekeeping y generación
 * de documentos) se declaran en la tarea 03.</p>
 */
@Configuration
public class RabbitMQConfig {

    private final MessagingProperties messaging;

    public RabbitMQConfig(MessagingProperties messaging) {
        this.messaging = messaging;
    }

    // ── Dead Letter Exchange / Queue ─────────────────────────────────────────

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(messaging.getDlx(), true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(messaging.getDlq()).build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(messaging.getDlqRoutingKey());
    }

    // ── Exchange principal ───────────────────────────────────────────────────

    @Bean
    public DirectExchange notificationsExchange() {
        return new DirectExchange(messaging.getExchange(), true, false);
    }

    // ── Colas de notificaciones (una por canal) ───────────────────────────────

    @Bean
    public Queue emailQueue() {
        return notificationQueue(messaging.getQueues().getNotifyEmail());
    }

    @Bean
    public Queue smsQueue() {
        return notificationQueue(messaging.getQueues().getNotifySms());
    }

    @Bean
    public Queue pushQueue() {
        return notificationQueue(messaging.getQueues().getNotifyPush());
    }

    /**
     * Cola durable con Dead Letter Exchange asociado: los mensajes rechazados o
     * agotados se reenvían al DLX con la routing key de la DLQ.
     */
    private Queue notificationQueue(String name) {
        return QueueBuilder.durable(name)
                .withArgument("x-dead-letter-exchange", messaging.getDlx())
                .withArgument("x-dead-letter-routing-key", messaging.getDlqRoutingKey())
                .build();
    }

    // ── Bindings: cola -> exchange principal ─────────────────────────────────

    @Bean
    public Binding emailBinding(Queue emailQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(emailQueue).to(notificationsExchange)
                .with(messaging.getRoutingKeys().getNotifyEmail());
    }

    @Bean
    public Binding smsBinding(Queue smsQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(smsQueue).to(notificationsExchange)
                .with(messaging.getRoutingKeys().getNotifySms());
    }

    @Bean
    public Binding pushBinding(Queue pushQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(pushQueue).to(notificationsExchange)
                .with(messaging.getRoutingKeys().getNotifyPush());
    }

    // ── Conversión de mensajes y template ────────────────────────────────────

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
