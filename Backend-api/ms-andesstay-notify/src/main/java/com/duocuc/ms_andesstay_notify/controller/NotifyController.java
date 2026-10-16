package com.duocuc.ms_andesstay_notify.controller;

import com.duocuc.ms_andesstay_notify.dto.SendNotificationRequest;
import com.duocuc.ms_andesstay_notify.messaging.documents.DocumentPublisher;
import com.duocuc.ms_andesstay_notify.messaging.housekeeping.HousekeepingTicketPublisher;
import com.duocuc.ms_andesstay_notify.messaging.notifications.NotificationPublisher;
import com.duocuc.ms_andesstay_notify.model.DocumentEvent;
import com.duocuc.ms_andesstay_notify.model.TicketEvent;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API REST de entrada de {@code notify}.
 *
 * <p>Este controlador <b>no conoce RabbitMQ</b>: valida el DTO y delega en el
 * publisher del dominio correspondiente. Ni {@code RabbitTemplate}, ni
 * {@code AmqpTemplate}, ni routing keys aparecen aquí (indicador 3).</p>
 */
@RestController
@RequestMapping("/api/notify")
@CrossOrigin(origins = "*")
public class NotifyController {

    private final NotificationPublisher notificationPublisher;
    private final HousekeepingTicketPublisher housekeepingTicketPublisher;
    private final DocumentPublisher documentPublisher;

    public NotifyController(NotificationPublisher notificationPublisher,
                            HousekeepingTicketPublisher housekeepingTicketPublisher,
                            DocumentPublisher documentPublisher) {
        this.notificationPublisher = notificationPublisher;
        this.housekeepingTicketPublisher = housekeepingTicketPublisher;
        this.documentPublisher = documentPublisher;
    }

    /** Notificaciones: publica en la cola del canal indicado (EMAIL, SMS o PUSH). */
    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@Valid @RequestBody SendNotificationRequest request) {
        String routingKey = notificationPublisher.publish(request.toEvent());
        return ResponseEntity.ok("Notificación enviada a RabbitMQ con routingKey: " + routingKey);
    }

    /** Tickets de housekeeping: endpoint de prueba (el productor real es reservations). */
    @PostMapping("/tickets")
    public ResponseEntity<String> sendTicket(@Valid @RequestBody TicketEvent ticket) {
        housekeepingTicketPublisher.publish(ticket);
        return ResponseEntity.ok("Ticket de housekeeping encolado: " + ticket.getTicketId());
    }

    /** Documentos (voucher/factura): endpoint de prueba (el productor real es reservations). */
    @PostMapping("/documents")
    public ResponseEntity<String> sendDocument(@Valid @RequestBody DocumentEvent document) {
        documentPublisher.publish(document);
        return ResponseEntity.ok("Solicitud de documento encolada: " + document.getDocumentId());
    }
}
