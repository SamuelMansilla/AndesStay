package com.duocuc.ms_andesstay_report.repository;

import com.duocuc.ms_andesstay_report.model.ReservationKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ReservationKpiRepository extends JpaRepository<ReservationKpi, Long> {

    List<ReservationKpi> findByReservationId(Long reservationId);

    Optional<ReservationKpi> findFirstByReservationId(Long reservationId);

    // Contar reservas agrupadas por estado en un rango de fechas
    @Query("SELECT r.status as status, COUNT(r) as total " +
           "FROM ReservationKpi r " +
           "WHERE COALESCE(r.createdAt, r.registeredAt, r.eventTimestamp) >= :startDate " +
           "GROUP BY r.status")
    List<Map<String, Object>> countReservationsByStatusSince(@Param("startDate") LocalDateTime startDate);

    // Contar las unidades con mayor demanda
    @Query("SELECT r.unitId as unitId, COUNT(r) as totalReservas " +
           "FROM ReservationKpi r " +
           "WHERE COALESCE(r.createdAt, r.registeredAt, r.eventTimestamp) >= :startDate " +
           "GROUP BY r.unitId " +
           "ORDER BY COUNT(r) DESC")
    List<Map<String, Object>> findTopDemandedUnitsSince(@Param("startDate") LocalDateTime startDate);

    // Contar reservas creadas desde una fecha específica (e.g. hoy)
    @Query("SELECT COUNT(r) FROM ReservationKpi r " +
           "WHERE COALESCE(r.createdAt, r.registeredAt) >= :startDate")
    long countReservationsCreatedSince(@Param("startDate") LocalDateTime startDate);

    // Contar reservas en un rango temporal
    @Query("SELECT COUNT(r) FROM ReservationKpi r " +
           "WHERE COALESCE(r.createdAt, r.registeredAt, r.eventTimestamp) >= :startDate")
    long countReservationsSince(@Param("startDate") LocalDateTime startDate);

    // Contar reservas activas actualmente para ocupación
    @Query("SELECT COUNT(r) FROM ReservationKpi r WHERE r.status IN :statuses")
    long countActiveReservations(@Param("statuses") List<String> statuses);

    // Métodos heredados para retrocompatibilidad
    long countByEventTimestampGreaterThanEqual(LocalDateTime startDate);
    long countByStatusInAndEventTimestampGreaterThanEqual(List<String> statuses, LocalDateTime startDate);
}