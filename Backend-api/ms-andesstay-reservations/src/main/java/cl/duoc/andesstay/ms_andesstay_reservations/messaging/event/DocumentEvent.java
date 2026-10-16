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
 * Solicitud de generación de documento (voucher o factura) que
 * {@code reservations} publica al crear o confirmar una reserva.
 *
 * <p>El payload se publica en {@code q.docs.generate} y lo consume
 * {@code ms-andesstay-notify} (dominio documents). Los nombres de los campos
 * coinciden con el modelo del consumidor; cada microservicio mantiene su propia
 * copia del evento.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentEvent implements Serializable {

    private String documentId;

    private Long reservationId;

    /** Tipo de documento: {@code VOUCHER} al crear, {@code FACTURA} al confirmar. */
    private String documentType;

    /** Correo del huésped al que se envía el documento. */
    private String recipient;

    private LocalDateTime createdAt;

    /** Solicitud de documento del tipo indicado para la reserva. */
    public static DocumentEvent of(Long reservationId, String documentType, String recipient) {
        return DocumentEvent.builder()
                .documentId(UUID.randomUUID().toString())
                .reservationId(reservationId)
                .documentType(documentType)
                .recipient(recipient)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
