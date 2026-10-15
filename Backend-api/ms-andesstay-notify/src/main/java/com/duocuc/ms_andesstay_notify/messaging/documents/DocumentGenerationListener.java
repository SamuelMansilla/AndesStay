package com.duocuc.ms_andesstay_notify.messaging.documents;

import com.duocuc.ms_andesstay_notify.messaging.AbstractDlqListener;
import com.duocuc.ms_andesstay_notify.model.DocumentEvent;
import com.duocuc.ms_andesstay_notify.service.DocumentService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Consumidor del dominio <b>DOCUMENTS</b> — vouchers y facturas.
 *
 * <ul>
 *   <li><b>Cola:</b> {@code q.docs.generate}</li>
 *   <li><b>Routing key:</b> {@code docs.generate}</li>
 *   <li><b>ACK:</b> manual ({@link AbstractDlqListener}): {@code basicAck} al
 *       generar el documento y descarte a la DLQ si el payload es inválido o se
 *       agotan los reintentos (tarea 05).</li>
 *   <li><b>Productor:</b> {@code reservations} al crear/confirmar la reserva
 *       (tarea 07).</li>
 * </ul>
 */
@Service
public class DocumentGenerationListener extends AbstractDlqListener<DocumentEvent> {

    private final DocumentService documentService;

    public DocumentGenerationListener(RetryTemplate messagingRetryTemplate,
                                      DocumentService documentService) {
        super(messagingRetryTemplate);
        this.documentService = documentService;
    }

    @RabbitListener(queues = "${andesstay.messaging.queues.docs-generate}")
    public void onDocument(DocumentEvent document, Channel channel,
                           @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                           @Header(AmqpHeaders.REDELIVERED) boolean redelivered) throws IOException {
        handle("DOCUMENTS", document, channel, deliveryTag, redelivered);
    }

    @Override
    protected void process(DocumentEvent document) {
        documentService.generateDocument(document);
    }
}
