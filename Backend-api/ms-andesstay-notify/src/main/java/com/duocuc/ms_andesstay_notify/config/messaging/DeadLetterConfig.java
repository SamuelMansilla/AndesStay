package com.duocuc.ms_andesstay_notify.config.messaging;

import com.duocuc.ms_andesstay_notify.config.MessagingProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dominio transversal: <b>Dead Letter</b> (mensajes descartados).
 *
 * <p>Ruta declarada:</p>
 * <pre>
 *   ex.andesstay.dlx  --[dlq.routing.key]--&gt;  q.andesstay.dlq
 * </pre>
 *
 * <p>Todas las colas de todos los dominios apuntan su dead-lettering a este DLX
 * (ver {@link TopologySupport}). Los nombres se leen de {@code application.yml}.</p>
 */
@Configuration
public class DeadLetterConfig {

    private final MessagingProperties messaging;

    public DeadLetterConfig(MessagingProperties messaging) {
        this.messaging = messaging;
    }

    /** DLX: exchange al que reencaminan las colas sus mensajes descartados. */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(messaging.getDlx(), true, false);
    }

    /** DLQ compartida: concentra los descartes para su analisis y alerta. */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(messaging.getDlq()).build();
    }

    /**
     * Binding de la DLQ.
     * Ruta: {@code ex.andesstay.dlx --[dlq.routing.key]--> q.andesstay.dlq}
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(messaging.getDlqRoutingKey());
    }
}
