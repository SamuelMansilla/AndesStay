package com.duocuc.ms_andesstay_notify.messaging.documents;

import com.duocuc.ms_andesstay_notify.config.MessagingProperties;
import com.duocuc.ms_andesstay_notify.messaging.AbstractMessagingPublisher;
import com.duocuc.ms_andesstay_notify.model.DocumentEvent;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;

/**
 * Dominio <b>DOCUMENTS</b>: publica solicitudes de generación de documentos
 * (vouchers y facturas).
 *
 * <p>Ruta publicada: routing key {@code docs.generate} hacia
 * {@code q.docs.generate} (exchange principal). El productor principal es
 * {@code reservations} al crear/confirmar una reserva; este publisher permite
 * también emitirlos desde {@code notify}.</p>
 */
@Component
public class DocumentPublisher extends AbstractMessagingPublisher {

    public DocumentPublisher(AmqpTemplate amqpTemplate, MessagingProperties messaging) {
        super(amqpTemplate, messaging);
    }

    /** Publica una solicitud de generación de documento. */
    public void publish(DocumentEvent document) {
        publish(messaging.getRoutingKeys().getDocsGenerate(), document);
    }
}
