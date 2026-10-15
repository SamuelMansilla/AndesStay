package com.duocuc.ms_andesstay_notify.config.messaging;

import com.duocuc.ms_andesstay_notify.config.MessagingProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dominio <b>DOCUMENTS</b> (caso de uso: generar voucher/factura de una reserva).
 *
 * <p>Ruta declarada (con dead-lettering hacia el DLX):</p>
 * <pre>
 *   ex.andesstay.notifications  --[docs.generate]--&gt;  q.docs.generate
 * </pre>
 *
 * <p>Productor: {@code reservations} al crear/confirmar una reserva (tarea 07).
 * Consumidor: {@code notify} / {@code report} (tarea 06).</p>
 */
@Configuration
public class DocumentsTopologyConfig {

    private final MessagingProperties messaging;

    public DocumentsTopologyConfig(MessagingProperties messaging) {
        this.messaging = messaging;
    }

    /** Cola de generacion de documentos (vouchers y facturas). */
    @Bean
    public Queue docsGenerateQueue() {
        return TopologySupport.durableWithDeadLetter(messaging.getQueues().getDocsGenerate(), messaging);
    }

    /** Ruta: ex.andesstay.notifications --[docs.generate]--> q.docs.generate */
    @Bean
    public Binding docsGenerateBinding(Queue docsGenerateQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(docsGenerateQueue).to(notificationsExchange)
                .with(messaging.getRoutingKeys().getDocsGenerate());
    }
}
