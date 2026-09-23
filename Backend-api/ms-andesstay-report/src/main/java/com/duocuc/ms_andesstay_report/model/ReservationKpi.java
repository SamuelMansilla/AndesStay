package com.duocuc.ms_andesstay_report.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_reservation_kpis")
public class ReservationKpi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reservationId;

    @Column(nullable = false)
    private Long unitId;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime eventTimestamp;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    public ReservationKpi() {
    }

    public ReservationKpi(Long reservationId, Long unitId, String status, LocalDateTime eventTimestamp) {
        this(reservationId, unitId, status, eventTimestamp, eventTimestamp);
    }

    public ReservationKpi(Long reservationId, Long unitId, String status, LocalDateTime eventTimestamp, LocalDateTime createdAt) {
        this.reservationId = reservationId;
        this.unitId = unitId;
        this.status = status;
        this.eventTimestamp = eventTimestamp;
        this.createdAt = createdAt != null ? createdAt : eventTimestamp;
    }

    @PrePersist
    public void prePersist() {
        this.registeredAt = LocalDateTime.now();
        if (this.eventTimestamp == null) {
            this.eventTimestamp = LocalDateTime.now();
        }
        if (this.createdAt == null) {
            this.createdAt = this.eventTimestamp;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }
}