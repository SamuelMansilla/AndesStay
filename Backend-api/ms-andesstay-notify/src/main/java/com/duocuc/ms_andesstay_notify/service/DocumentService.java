package com.duocuc.ms_andesstay_notify.service;

import com.duocuc.ms_andesstay_notify.messaging.exception.NonRecoverableException;
import com.duocuc.ms_andesstay_notify.model.DocumentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio del dominio <b>DOCUMENTS</b>: vouchers y facturas de una
 * reserva.
 *
 * <p>No conoce RabbitMQ; el listener de documentos es el que aplica el contrato
 * de ACK (indicador 4).</p>
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    /**
     * Genera el documento solicitado (voucher o factura) para la reserva.
     *
     * <p>En el alcance actual de la EP la generación se registra; el envío real
     * del archivo al huésped se resuelve en el dominio de notificaciones.</p>
     */
    public void generateDocument(DocumentEvent document) {
        requireValid(document);
        log.info("[DOCS] Documento {} ({}) generado para la reserva #{} y enviado a {}",
                document.getDocumentId(), document.getDocumentType(),
                document.getReservationId(), document.getRecipient());
    }

    /** Un documento sin reserva, tipo o destinatario no podrá generarse: no recuperable. */
    private void requireValid(DocumentEvent document) {
        if (document == null) {
            throw new NonRecoverableException("Payload nulo en DOCUMENTS");
        }
        if (document.getReservationId() == null) {
            throw new NonRecoverableException("Documento sin reserva (reservationId)");
        }
        if (isBlank(document.getDocumentType())) {
            throw new NonRecoverableException("Documento sin tipo (documentType)");
        }
        if (isBlank(document.getRecipient())) {
            throw new NonRecoverableException("Documento sin destinatario (recipient)");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
