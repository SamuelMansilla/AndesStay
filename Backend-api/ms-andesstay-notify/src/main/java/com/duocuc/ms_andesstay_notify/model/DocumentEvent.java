package com.duocuc.ms_andesstay_notify.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Solicitud de generación de documento (voucher o factura) de una reserva.
 *
 * <p>Se publica en la cola del dominio documents ({@code q.docs.generate}) con la
 * routing key {@code docs.generate}. Productor principal: {@code reservations}
 * al crear/confirmar (tarea 07).</p>
 */
public class DocumentEvent implements Serializable {

    private String documentId;

    @NotNull(message = "reservationId es obligatorio")
    private Long reservationId;

    @NotBlank(message = "documentType es obligatorio (VOUCHER o FACTURA)")
    private String documentType;

    @NotBlank(message = "recipient es obligatorio")
    private String recipient;

    private LocalDateTime createdAt;

    public DocumentEvent() {
        this.documentId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
