package com.duocuc.ms_andesstay_notify.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class NotificationEvent implements Serializable {

    private String notificationId;
    private Long reservationId;
    private String recipient;
    private String channel; // EMAIL, SMS, PUSH
    private String subject;
    private String body;
    private LocalDateTime timestamp;

    public NotificationEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public NotificationEvent(String notificationId, Long reservationId, String recipient, String channel, String subject, String body) {
        this.notificationId = notificationId;
        this.reservationId = reservationId;
        this.recipient = recipient;
        this.channel = channel;
        this.subject = subject;
        this.body = body;
        this.timestamp = LocalDateTime.now();
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

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

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}