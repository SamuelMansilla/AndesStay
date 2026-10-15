package com.duocuc.ms_andesstay_notify.config.messaging;

import com.duocuc.ms_andesstay_notify.config.MessagingProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;

/**
 * Utilidades compartidas por las clases de topología de cada dominio.
 *
 * <p>Evita repetir los argumentos de dead-lettering en cada cola: los nombres
 * del DLX y de la routing key de la DLQ salen siempre de
 * {@link MessagingProperties} (nunca como literal).</p>
 */
final class TopologySupport {

    private TopologySupport() {
    }

    /**
     * Cola durable con dead-lettering configurado: todo mensaje rechazado o que
     * agota sus reintentos se reenvía al DLX con la routing key de la DLQ.
     *
     * @param queueName nombre de la cola, tomado de la configuración
     * @param messaging propiedades de mensajería con DLX y routing key de la DLQ
     */
    static Queue durableWithDeadLetter(String queueName, MessagingProperties messaging) {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", messaging.getDlx())
                .withArgument("x-dead-letter-routing-key", messaging.getDlqRoutingKey())
                .build();
    }
}
