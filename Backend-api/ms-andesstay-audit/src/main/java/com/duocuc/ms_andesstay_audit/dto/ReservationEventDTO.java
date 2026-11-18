package com.duocuc.ms_andesstay_audit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Contrato del evento de reserva consumido desde {@code audit.timeline}.
 *
 * <p>Debe mantenerse idéntico al DTO publicado por {@code ms-andesstay-reservations}.
 * Los campos obligatorios se validan con Bean Validation antes de persistir.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReservationEventDTO(
        @NotBlank String eventId,
        @NotBlank String eventType,
        String type,
        String timestamp,
        String createdAt,
        String traceId,
        String correlationId,
        String actor,
        @NotNull @Positive Long reservationId,
        @Positive Long unitId,
        String status,
        String details) {
}
