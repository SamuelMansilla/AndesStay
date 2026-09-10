package cl.duoc.andesstay.ms_andesstay_reservations.dto;

import cl.duoc.andesstay.ms_andesstay_reservations.entity.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusDTO {

    @NotNull(message = "El nuevo estado es requerido")
    private ReservationStatus status;
}
