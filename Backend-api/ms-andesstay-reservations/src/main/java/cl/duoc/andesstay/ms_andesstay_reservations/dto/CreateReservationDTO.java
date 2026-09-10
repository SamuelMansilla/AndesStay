package cl.duoc.andesstay.ms_andesstay_reservations.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateReservationDTO {

    @NotNull(message = "El email del huésped es obligatorio")
    @Email(message = "El email debe ser válido")
    private String guestEmail;

    @NotNull(message = "El identificador de unidad es obligatorio")
    private Long unitId;

    @NotNull(message = "La fecha de check-in es obligatoria")
    @FutureOrPresent(message = "La fecha de check-in no puede ser en el pasado")
    private LocalDate checkInDate;

    @NotNull(message = "La fecha de check-out es obligatoria")
    private LocalDate checkOutDate;
}