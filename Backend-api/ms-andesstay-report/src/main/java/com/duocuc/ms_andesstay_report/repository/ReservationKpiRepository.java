package com.duocuc.ms_andesstay_report.repository;

import com.duocuc.ms_andesstay_report.model.ReservationKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface ReservationKpiRepository extends JpaRepository<ReservationKpi, Long> {

    // Contar reservas agrupadas por estado en un rango de fechas
    @Query("SELECT r.status as status, COUNT(r) as total " +
           "FROM ReservationKpi r " +
           "WHERE r.eventTimestamp >= :startDate " +
           "GROUP BY r.status")
    List<Map<String, Object>> countReservationsByStatusSince(@Param("startDate") LocalDateTime startDate);

    // Contar las unidades con mayor demanda
    @Query("SELECT r.unitId as unitId, COUNT(r) as totalReservas " +
           "FROM ReservationKpi r " +
           "WHERE r.eventTimestamp >= :startDate " +
           "GROUP BY r.unitId " +
           "ORDER BY COUNT(r) DESC")
    List<Map<String, Object>> findTopDemandedUnitsSince(@Param("startDate") LocalDateTime startDate);
}