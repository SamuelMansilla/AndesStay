package cl.duoc.andesstay.ms_andesstay_reservations.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ReservationStatus {
    CREADA,
    CONFIRMADA,
    CHECKIN_PENDIENTE,
    EN_ESTADIA,
    CHECKOUT,
    CANCELADA;

    @JsonCreator
    public static ReservationStatus fromString(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase();
        if ("EN_ESTADÍA".equals(normalized) || "EN_ESTADIA".equals(normalized)) {
            return EN_ESTADIA;
        }
        for (ReservationStatus status : ReservationStatus.values()) {
            if (status.name().equalsIgnoreCase(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Estado de reserva inválido: " + value);
    }
}