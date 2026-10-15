package com.duocuc.ms_andesstay_notify.config;

import com.duocuc.ms_andesstay_notify.messaging.exception.RecoverableException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

/**
 * Configuración de los consumidores ({@code @RabbitListener}) — indicador 5.
 *
 * <p>Se configura en Java (y no en {@code spring.rabbitmq.listener.*}) porque al
 * declarar este factory se sustituye el de Spring Boot: los valores del
 * {@code application.yml} serían ignorados y la configuración quedaría repartida
 * y engañosa. Todo lo relevante del consumo se ve aquí.</p>
 */
@Configuration
public class RabbitListenerConfig {

    /** Intentos totales por mensaje antes de descartarlo a la DLQ. */
    public static final int MAX_ATTEMPTS = 3;

    /** Espera entre reintentos (ms). */
    public static final long RETRY_INTERVAL_MS = 2000L;

    /**
     * Factory de los {@code @RabbitListener}.
     *
     * <ul>
     *   <li>{@code MANUAL}: el listener confirma con {@code basicAck} /
     *       {@code basicNack} (ver {@code AbstractDlqListener}).</li>
     *   <li>{@code defaultRequeueRejected=false}: ante un rechazo, el mensaje
     *       nunca vuelve a la cola original: va a la DLQ.</li>
     *   <li>{@code missingQueuesFatal=false}: si falta una cola, el arranque no
     *       falla (se reintenta la declaración).</li>
     * </ul>
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        factory.setMissingQueuesFatal(false);
        factory.setConcurrentConsumers(2);
        factory.setPrefetchCount(10);
        return factory;
    }

    /**
     * Plantilla de <b>reintentos acotados</b>: {@link #MAX_ATTEMPTS} intentos
     * separados por {@link #RETRY_INTERVAL_MS} ms.
     *
     * <p><b>Sólo</b> reintenta errores recuperables: el
     * {@link SimpleRetryPolicy} se declara con {@code defaultValue=false}, así
     * que cualquier otra excepción (incluida {@code NonRecoverableException}) se
     * propaga de inmediato al contrato de ACK en lugar de reintentarse.</p>
     */
    @Bean
    public RetryTemplate messagingRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(
                MAX_ATTEMPTS,
                Map.of(RecoverableException.class, true),
                false,   // no inspeccionar las causas encadenadas
                false)); // cualquier otra excepción no se reintenta

        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(RETRY_INTERVAL_MS);
        retryTemplate.setBackOffPolicy(backOff);
        return retryTemplate;
    }
}
