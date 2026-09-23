package com.duocuc.ms_andesstay_audit.repository;

import com.duocuc.ms_andesstay_audit.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByTimestampDesc();

    List<AuditLog> findByActorIgnoreCaseOrderByTimestampDesc(String actor);

    List<AuditLog> findByReservationIdOrderByTimestampDesc(Long reservationId);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:actor IS NULL OR LOWER(a.actor) LIKE LOWER(CONCAT('%', :actor, '%'))) AND " +
           "(:eventType IS NULL OR LOWER(a.eventType) LIKE LOWER(CONCAT('%', :eventType, '%'))) AND " +
           "(:reservationId IS NULL OR a.reservationId = :reservationId) " +
           "ORDER BY a.timestamp DESC")
    List<AuditLog> searchLogs(
            @Param("actor") String actor,
            @Param("eventType") String eventType,
            @Param("reservationId") Long reservationId);
}