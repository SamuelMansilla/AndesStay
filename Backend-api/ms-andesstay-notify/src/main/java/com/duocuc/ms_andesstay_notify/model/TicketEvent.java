package com.duocuc.ms_andesstay_notify.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ticket de aseo generado al cerrar una reserva (CHECKOUT).
 *
 * <p>Se publica en la cola del dominio housekeeping ({@code q.housekeeping.tickets})
 * con la routing key {@code housekeeping.ticket}. Productor principal:
 * {@code reservations} (tarea 07).</p>
 */
public class TicketEvent implements Serializable {

    private String ticketId;

    @NotNull(message = "reservationId es obligatorio")
    private Long reservationId;

    @NotNull(message = "unitId es obligatorio")
    private Long unitId;

    @NotBlank(message = "type es obligatorio (por ejemplo LIMPIEZA)")
    private String type;

    private String notes;

    private LocalDateTime createdAt;

    public TicketEvent() {
        this.ticketId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
