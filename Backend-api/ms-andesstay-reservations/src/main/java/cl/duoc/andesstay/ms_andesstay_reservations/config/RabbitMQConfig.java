package cl.duoc.andesstay.ms_andesstay_reservations.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración AMQP de {@code reservations} (productor).
 *
 * <p>Declara el converter JSON y el {@link RabbitTemplate} que usan los
 * publishers de dominio. La <b>topología no se declara aquí</b>: el dueño de la
 * declaración de exchange, colas y bindings es {@code ms-andesstay-notify}
 * (tarea 03), para no tener dos servicios compitiendo por la misma definición.
 * Este servicio sólo publica usando los nombres que lee de
 * {@link MessagingProperties}.</p>
 */
@Configuration
public class RabbitMQConfig {

    /** Serializa los eventos de negocio como JSON (mismo formato que consume notify). */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /** Template de publicación con el converter JSON. */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
