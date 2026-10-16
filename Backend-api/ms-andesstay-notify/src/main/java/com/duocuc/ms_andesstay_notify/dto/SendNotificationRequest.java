package com.duocuc.ms_andesstay_notify.dto;

import com.duocuc.ms_andesstay_notify.model.NotificationEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de entrada de {@code POST /api/notify/send}.
 *
 * <p>Se valida con Jakarta Validation (indicador 8) antes de convertirse en el
 * {@link NotificationEvent} que publica el {@code NotificationPublisher}.</p>
 */
public class SendNotificationRequest {

    @NotNull(message = "reservationId es obligatorio")
    @Positive(message = "reservationId debe ser positivo")
    private Long reservationId;

    @NotBlank(message = "recipient es obligatorio")
    @Size(max = 200, message = "recipient no puede superar los 200 caracteres")
    private String recipient;

    @NotBlank(message = "channel es obligatorio")
    @Pattern(regexp = "(?i)EMAIL|SMS|PUSH", message = "channel debe ser EMAIL, SMS o PUSH")
    private String channel;

    @Size(max = 200, message = "subject no puede superar los 200 caracteres")
    private String subject;

    @NotBlank(message = "body es obligatorio")
    private String body;

    /**
     * Convierte el DTO ya validado en el evento que viaja por RabbitMQ,
     * generando su identificador y sello de tiempo.
     */
    public NotificationEvent toEvent() {
        NotificationEvent event = new NotificationEvent();
        event.setNotificationId(UUID.randomUUID().toString());
        event.setReservationId(reservationId);
        event.setRecipient(recipient);
        event.setChannel(channel == null ? null : channel.trim().toUpperCase());
        event.setSubject(subject);
        event.setBody(body);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
