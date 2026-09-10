package cl.duoc.andesstay.ms_andesstay_reservations.controller;

import cl.duoc.andesstay.ms_andesstay_reservations.dto.CreateReservationDTO;
import cl.duoc.andesstay.ms_andesstay_reservations.dto.UpdateStatusDTO;
import cl.duoc.andesstay.ms_andesstay_reservations.entity.Reservation;
import cl.duoc.andesstay.ms_andesstay_reservations.entity.ReservationStatus;
import cl.duoc.andesstay.ms_andesstay_reservations.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<Reservation> create(@Valid @RequestBody CreateReservationDTO dto) {
        Reservation created = reservationService.createReservation(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<Reservation>> list(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reservationService.getReservations(status, from, to));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reservation> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Reservation> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusDTO dto) {
        return ResponseEntity.ok(reservationService.updateStatus(id, dto));
    }
}