package cl.duoc.andesstay.ms_andesstay_reservations.messaging;

import cl.duoc.andesstay.ms_andesstay_reservations.config.MessagingProperties;
import cl.duoc.andesstay.ms_andesstay_reservations.messaging.event.DocumentEvent;
import cl.duoc.andesstay.ms_andesstay_reservations.messaging.event.TicketEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Productor de eventos de negocio de {@code reservations} hacia RabbitMQ.
 *
 * <p>Reglas del componente (indicadores 1 y 3):</p>
 * <ul>
 *   <li>Los nombres de exchange y routing keys salen de
 *       {@link MessagingProperties}, nunca como literales.</li>
 *   <li>Expone métodos de alto nivel por caso de uso; la lógica de reservas no
 *       conoce ni el exchange ni las routing keys.</li>
 *   <li><b>Nunca bloquea ni rompe el core</b>: la publicación corre en un hilo
 *       aparte y cualquier fallo se registra como {@code WARN}. Si el broker está
 *       caído, el endpoint responde igual.</li>
 * </ul>
 */
@Component
public class ReservationMessagingPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReservationMessagingPublisher.class);

    private static final String VOUCHER = "VOUCHER";
    private static final String FACTURA = "FACTURA";

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties messaging;

    public ReservationMessagingPublisher(RabbitTemplate rabbitTemplate, MessagingProperties messaging) {
        this.rabbitTemplate = rabbitTemplate;
        this.messaging = messaging;
    }

    /**
     * Publica el ticket de aseo que genera el CHECKOUT de una reserva.
     *
     * <p>Ruta: exchange principal → routing key {@code housekeeping.ticket} →
     * {@code q.housekeeping.tickets}.</p>
     */
    public void publishTicketEvent(Long reservationId, Long unitId) {
        publish(messaging.getRoutingKeys().getHousekeepingTicket(),
                TicketEvent.forCheckout(reservationId, unitId),
                "ticket de housekeeping de la reserva #" + reservationId);
    }

    /**
     * Publica la solicitud del voucher de una reserva recién creada.
     *
     * <p>Ruta: exchange principal → routing key {@code docs.generate} →
     * {@code q.docs.generate}.</p>
     */
    public void publishVoucherEvent(Long reservationId, String guestEmail) {
        publishDocument(reservationId, guestEmail, VOUCHER);
    }

    /**
     * Publica la solicitud de la factura de una reserva confirmada.
     *
     * <p>Ruta: exchange principal → routing key {@code docs.generate} →
     * {@code q.docs.generate}.</p>
     */
    public void publishInvoiceEvent(Long reservationId, String guestEmail) {
        publishDocument(reservationId, guestEmail, FACTURA);
    }

    private void publishDocument(Long reservationId, String guestEmail, String documentType) {
        publish(messaging.getRoutingKeys().getDocsGenerate(),
                DocumentEvent.of(reservationId, documentType, guestEmail),
                documentType + " de la reserva #" + reservationId);
    }

    /**
     * Envía el evento sin bloquear el hilo de la petición HTTP.
     *
     * <p>El resultado se registra siempre: {@code INFO} si se publicó y
     * {@code WARN} si el broker no estaba disponible. La excepción nunca se
     * propaga al flujo de negocio (regla del proyecto: no bloquear el core).</p>
     */
    private void publish(String routingKey, Object event, String description) {
        CompletableFuture.runAsync(() -> {
            try {
                rabbitTemplate.convertAndSend(messaging.getExchange(), routingKey, event);
                log.info("[RABBITMQ] Publicado {} en el exchange {} con routing key {} (evento: {})",
                        description, messaging.getExchange(), routingKey, event);
            } catch (Exception e) {
                log.warn("[RABBITMQ] No se pudo publicar {} (no bloquea el core): {}",
                        description, e.getMessage());
            }
        });
    }
}
