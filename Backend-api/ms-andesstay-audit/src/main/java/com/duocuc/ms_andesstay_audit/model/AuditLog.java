package com.duocuc.ms_andesstay_audit.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType; // Ej: RESERVA_CREADA, RESERVA_CONFIRMADA

    @Column(nullable = false)
    private String actor; // Quién hizo la acción (usuario o sistema)

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 500)
    private String details;

    @Column(length = 2000)
    private String payload; // Detalles en formato JSON o texto

    public AuditLog() {}

    public AuditLog(String eventType, String actor, LocalDateTime timestamp, String payload) {
        this.eventType = eventType;
        this.actor = actor;
        this.timestamp = timestamp;
        this.payload = payload;
        this.details = payload;
    }

    public AuditLog(String eventType, String actor, Long reservationId, LocalDateTime timestamp, String details, String payload) {
        this.eventType = eventType;
        this.actor = actor;
        this.reservationId = reservationId;
        this.timestamp = timestamp;
        this.details = details;
        this.payload = payload;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getDetails() { return details != null ? details : payload; }
    public void setDetails(String details) { this.details = details; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    // Métodos alias para compatibilidad directa con el Frontend (AuditEvent)
    public String getAction() { return eventType; }
    public String getPerformedBy() { return actor; }
}