package cl.duoc.andesstay.ms_andesstay_reservations.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ticket de aseo que {@code reservations} publica al pasar una reserva a
 * CHECKOUT.
 *
 * <p>El payload se publica en {@code q.housekeeping.tickets} y lo consume
 * {@code ms-andesstay-notify} (dominio housekeeping). Los nombres de los campos
 * coinciden con el modelo del consumidor para que el JSON se bindee sin
 * ambigüedad; cada microservicio mantiene su propia copia del evento en vez de
 * compartir un artefacto común.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketEvent implements Serializable {

    private String ticketId;

    private Long reservationId;

    private Long unitId;

    /** Tipo de ticket; por ahora el checkout genera uno de limpieza. */
    private String type;

    private String notes;

    private LocalDateTime createdAt;

    /** Ticket de limpieza para la reserva y unidad indicadas. */
    public static TicketEvent forCheckout(Long reservationId, Long unitId) {
        return TicketEvent.builder()
                .ticketId(UUID.randomUUID().toString())
                .reservationId(reservationId)
                .unitId(unitId)
                .type("LIMPIEZA")
                .notes("Generado automáticamente al cerrar la reserva #" + reservationId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
