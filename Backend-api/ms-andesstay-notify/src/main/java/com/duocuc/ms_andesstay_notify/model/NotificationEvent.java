package com.duocuc.ms_andesstay_notify.model;

import java.time.LocalDateTime;

public class NotificationEvent {

    private String type; // Ej: "email.send", "housekeeping.ticket"[cite: 2]
    private String eventId;
    private LocalDateTime timestamp;
    private String traceId;
    private String correlationId;
    private Object payload; // Los datos específicos del correo o ticket

    public NotificationEvent() {}

    // Getters y Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
}