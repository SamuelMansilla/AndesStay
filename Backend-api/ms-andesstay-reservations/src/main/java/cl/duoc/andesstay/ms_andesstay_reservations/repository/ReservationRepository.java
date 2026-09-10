package cl.duoc.andesstay.ms_andesstay_reservations.repository;

import cl.duoc.andesstay.ms_andesstay_reservations.entity.Reservation;
import cl.duoc.andesstay.ms_andesstay_reservations.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByStatus(ReservationStatus status);
    List<Reservation> findByGuestEmail(String guestEmail);
    List<Reservation> findByCheckInDateBetween(LocalDate from, LocalDate to);
}