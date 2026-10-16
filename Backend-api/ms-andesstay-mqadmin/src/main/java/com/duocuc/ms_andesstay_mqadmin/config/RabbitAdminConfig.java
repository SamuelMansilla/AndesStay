package com.duocuc.ms_andesstay_mqadmin.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del cliente de administración de RabbitMQ.
 *
 * <p>Se declara explícitamente el bean {@link AmqpAdmin} (una instancia de
 * {@link RabbitAdmin}) para dejar claro que este microservicio <b>solo</b>
 * administra el broker a petición (vía {@code RabbitAdminService}), y nunca
 * declara la topología de negocio de forma automática: {@code autoStartup} en
 * {@code false} evita que declare beans {@code Declarable} al arrancar, ya que
 * el dueño de esa topología es {@code ms-andesstay-notify}.</p>
 *
 * <p>Spring Boot ya autoconfigura un {@code AmqpAdmin}, de modo que este bean lo
 * sustituye de forma ordenada gracias a {@link ConditionalOnMissingBean}.</p>
 */
@Configuration
public class RabbitAdminConfig {

    /** Cliente {@code AmqpAdmin} que encapsula {@code RabbitAdminService} (indicador 7). */
    @Bean
    @ConditionalOnMissingBean(AmqpAdmin.class)
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        // No declarar automáticamente la topología de negocio; solo administrar
        // los recursos que se soliciten explícitamente por la API.
        rabbitAdmin.setAutoStartup(false);
        return rabbitAdmin;
    }
}
