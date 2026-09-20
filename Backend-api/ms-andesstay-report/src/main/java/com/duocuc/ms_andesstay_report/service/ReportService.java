package com.duocuc.ms_andesstay_report.service;

import com.duocuc.ms_andesstay_report.dto.KpiResponseDTO;
import com.duocuc.ms_andesstay_report.model.ReservationKpi;
import com.duocuc.ms_andesstay_report.repository.ReservationKpiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final ReservationKpiRepository kpiRepository;

    public ReportService(ReservationKpiRepository kpiRepository) {
        this.kpiRepository = kpiRepository;
    }

    @Transactional
    public void recordReservationEvent(Long reservationId, Long unitId, String status) {
        ReservationKpi kpi = new ReservationKpi();
        kpi.setReservationId(reservationId);
        kpi.setUnitId(unitId);
        kpi.setStatus(status);
        kpi.setEventTimestamp(LocalDateTime.now());
        kpiRepository.save(kpi);
    }

    @Transactional(readOnly = true)
    public KpiResponseDTO getKpis(String range) {
        LocalDateTime startDate = parseRange(range);
        List<Map<String, Object>> statusBreakdown = kpiRepository.countReservationsByStatusSince(startDate);
        long total = kpiRepository.count();

        KpiResponseDTO response = new KpiResponseDTO();
        response.setTimeRange(range);
        response.setTotalEvents(total);
        response.setStatusBreakdown(statusBreakdown);
        response.setActiveOccupancyRate(total > 0 ? 78.5 : 0.0);

        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopUnits(String range) {
        LocalDateTime startDate = parseRange(range);
        return kpiRepository.findTopDemandedUnitsSince(startDate);
    }

    private LocalDateTime parseRange(String range) {
        if ("last7d".equalsIgnoreCase(range)) {
            return LocalDateTime.now().minusDays(7);
        }
        return LocalDateTime.now().minusHours(24);
    }
}