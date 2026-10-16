package com.duocuc.ms_andesstay_notify.service;

import com.duocuc.ms_andesstay_notify.messaging.exception.NonRecoverableException;
import com.duocuc.ms_andesstay_notify.model.TicketEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio del dominio <b>HOUSEKEEPING</b>: los tickets de aseo.
 *
 * <p>No conoce RabbitMQ; el listener de housekeeping es el que aplica el
 * contrato de ACK (indicador 4).</p>
 */
@Service
public class HousekeepingService {

    private static final Logger log = LoggerFactory.getLogger(HousekeepingService.class);

    /**
     * Registra el ticket de aseo generado al cerrar (CHECKOUT) una reserva.
     *
     * <p>En el alcance actual de la EP el ticket sólo se registra; la asignación
     * de personal y el seguimiento quedan fuera de estas tareas.</p>
     */
    public void createTicket(TicketEvent ticket) {
        requireValid(ticket);
        log.info("[HOUSEKEEPING] Ticket {} ({}) registrado para la reserva #{} de la unidad #{}. Notas: {}",
                ticket.getTicketId(), ticket.getType(), ticket.getReservationId(),
                ticket.getUnitId(), ticket.getNotes());
    }

    /** Un ticket sin reserva, unidad o tipo no podrá procesarse nunca: no recuperable. */
    private void requireValid(TicketEvent ticket) {
        if (ticket == null) {
            throw new NonRecoverableException("Payload nulo en HOUSEKEEPING");
        }
        if (ticket.getReservationId() == null) {
            throw new NonRecoverableException("Ticket sin reserva (reservationId)");
        }
        if (ticket.getUnitId() == null) {
            throw new NonRecoverableException("Ticket sin unidad (unitId)");
        }
        if (isBlank(ticket.getType())) {
            throw new NonRecoverableException("Ticket sin tipo (type)");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
