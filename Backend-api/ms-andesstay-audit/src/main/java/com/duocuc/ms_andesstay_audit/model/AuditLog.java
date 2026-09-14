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

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 1000)
    private String payload; // Detalles en formato JSON o texto

    public AuditLog() {}

    public AuditLog(String eventType, String actor, LocalDateTime timestamp, String payload) {
        this.eventType = eventType;
        this.actor = actor;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}