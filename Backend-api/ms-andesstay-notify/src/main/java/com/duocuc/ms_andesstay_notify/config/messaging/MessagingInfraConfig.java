package com.duocuc.ms_andesstay_notify.config.messaging;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infraestructura comun de mensajeria (no depende del dominio).
 *
 * <p>Concentra el converter JSON, el template para publicar y el
 * {@link RabbitAdmin} que declara la topologia (todas las {@code Queue},
 * {@code Exchange} y {@code Binding} declaradas como beans) al arrancar.</p>
 */
@Configuration
public class MessagingInfraConfig {

    /** Serializa/deserializa los eventos como JSON (NotificationEvent, TicketEvent, ...). */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /** Template para publicar mensajes usando el converter JSON. */
    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    /**
     * Administrador de AMQP: declara exchanges, colas y bindings al arrancar y
     * permite inspeccionarlos. Sustituye al auto-configurado de Spring Boot de
     * forma explicita (mismo nombre de bean y misma semantica).
     */
    @Bean
    public RabbitAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
