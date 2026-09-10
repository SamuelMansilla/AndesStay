package cl.duoc.andesstay.ms_andesstay_reservations.service;

import cl.duoc.andesstay.ms_andesstay_reservations.dto.CreateReservationDTO;
import cl.duoc.andesstay.ms_andesstay_reservations.dto.UpdateStatusDTO;
import cl.duoc.andesstay.ms_andesstay_reservations.entity.Reservation;
import cl.duoc.andesstay.ms_andesstay_reservations.entity.ReservationStatus;
import cl.duoc.andesstay.ms_andesstay_reservations.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public ReservationService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
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

        return reservationRepository.save(reservation);
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
        return reservationRepository.save(reservation);
    }
}