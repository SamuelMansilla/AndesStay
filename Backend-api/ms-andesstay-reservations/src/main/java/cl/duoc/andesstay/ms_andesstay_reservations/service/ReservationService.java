package cl.duoc.andesstay.ms_andesstay_reservations.service;

import cl.duoc.andesstay.ms_andesstay_reservations.dto.CreateReservationDTO;
import cl.duoc.andesstay.ms_andesstay_reservations.dto.UpdateStatusDTO;
import cl.duoc.andesstay.ms_andesstay_reservations.entity.Reservation;
import cl.duoc.andesstay.ms_andesstay_reservations.entity.ReservationStatus;
import cl.duoc.andesstay.ms_andesstay_reservations.event.ReservationEventPublisher;
import cl.duoc.andesstay.ms_andesstay_reservations.messaging.ReservationMessagingPublisher;
import cl.duoc.andesstay.ms_andesstay_reservations.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationEventPublisher eventPublisher;
    private final ReservationMessagingPublisher messagingPublisher;

    public ReservationService(ReservationRepository reservationRepository,
                              ReservationEventPublisher eventPublisher,
                              ReservationMessagingPublisher messagingPublisher) {
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
        this.messagingPublisher = messagingPublisher;
    }

    @Transactional
    public Reservation createReservation(CreateReservationDTO dto) {
        if (dto.getCheckOutDate().isBefore(dto.getCheckInDate())) {
            throw new IllegalArgumentException("La fecha de check-out debe ser posterior al check-in");
        }

        Reservation reservation = Reservation.builder()
                .guestEmail(dto.getGuestEmail())
                .unitId(dto.getUnitId())
                .checkInDate(dto.getCheckInDate())
                .checkOutDate(dto.getCheckOutDate())
                .status(ReservationStatus.CREADA)
                .build();

        Reservation saved = reservationRepository.save(reservation);

        // Publicar evento de negocio (Kafka streaming + fallback local — no bloquea el core)
        eventPublisher.publishReservationEvent(
                saved.getId(),
                saved.getUnitId(),
                saved.getStatus().name(),
                saved.getGuestEmail(),
                "RESERVA_CREADA",
                "Reserva creada para unidad #" + saved.getUnitId() + " por " + saved.getGuestEmail(),
                saved.getCreatedAt()
        );

        // Evento de negocio hacia RabbitMQ: la reserva creada necesita su voucher.
        // Asíncrono y tolerante a fallos: si el broker no está, el endpoint responde igual.
        messagingPublisher.publishVoucherEvent(saved.getId(), saved.getGuestEmail());

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Reservation> getReservations(ReservationStatus status, LocalDate from, LocalDate to) {
        if (status != null) {
            return reservationRepository.findByStatus(status);
        }
        if (from != null && to != null) {
            return reservationRepository.findByCheckInDateBetween(from, to);
        }
        return reservationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Reservation getReservationById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con ID: " + id));
    }

    @Transactional
    public Reservation updateStatus(Long id, UpdateStatusDTO dto) {
        Reservation reservation = getReservationById(id);
        ReservationStatus nextStatus = dto.getStatus();

        // Regla clave: No se puede pasar a EN_ESTADIA (Check-in) sin estar CONFIRMADA
        if (nextStatus == ReservationStatus.EN_ESTADIA &&
            reservation.getStatus() != ReservationStatus.CONFIRMADA &&
            reservation.getStatus() != ReservationStatus.CHECKIN_PENDIENTE) {
            throw new IllegalStateException("No se puede realizar check-in de una reserva sin confirmar previa.");
        }

        reservation.setStatus(nextStatus);
        Reservation updated = reservationRepository.save(reservation);

        // Mapeo de estado a tipo de evento del Caso 5
        String eventType = switch (nextStatus) {
            case CONFIRMADA        -> "RESERVA_CONFIRMADA";
            case CHECKIN_PENDIENTE -> "CHECKIN_PENDIENTE";
            case EN_ESTADIA        -> "CHECK_IN";
            case CHECKOUT          -> "CHECK_OUT";
            case CANCELADA         -> "CANCELADA";
            default                -> "ESTADO_ACTUALIZADO";
        };

        // Publicar evento de negocio (Kafka streaming + fallback local — no bloquea el core)
        eventPublisher.publishReservationEvent(
                updated.getId(),
                updated.getUnitId(),
                updated.getStatus().name(),
                updated.getGuestEmail(),
                eventType,
                eventType + " para reserva #" + updated.getId() + " por " + updated.getGuestEmail(),
                updated.getCreatedAt()
        );

        // Eventos de negocio hacia RabbitMQ según la transición de estado
        // (asíncronos y tolerantes a fallos: nunca bloquean ni rompen el core)
        switch (nextStatus) {
            // Reserva confirmada: se emite la factura.
            case CONFIRMADA -> messagingPublisher.publishInvoiceEvent(updated.getId(), updated.getGuestEmail());
            // Checkout: se genera el ticket de aseo para la unidad.
            case CHECKOUT -> messagingPublisher.publishTicketEvent(updated.getId(), updated.getUnitId());
            default -> { /* sin evento de mensajería para esta transición */ }
        }

        return updated;
    }
}