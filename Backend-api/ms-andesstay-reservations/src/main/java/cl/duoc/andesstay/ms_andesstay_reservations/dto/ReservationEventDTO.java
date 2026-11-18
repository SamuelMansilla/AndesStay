package cl.duoc.andesstay.ms_andesstay_reservations.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Evento de reserva publicado en Kafka (EP5).
 *
 * <p>Es el contrato JSON que comparten {@code reservations} (productor),
 * {@code report} y {@code audit} (consumidores). Se usa un {@code record} para
 * que el DTO sea inmutable y el contrato quede explícito en el constructor.</p>
 *
 * <p>Campos obligatorios para la validación del consumidor: {@code eventId},
 * {@code eventType} y {@code reservationId}.</p>
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
