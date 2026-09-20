package com.duocuc.ms_andesstay_report.controller;

import com.duocuc.ms_andesstay_report.dto.KpiResponseDTO;
import com.duocuc.ms_andesstay_report.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * GET /api/report/kpis?range=last24h
     */
    @GetMapping("/kpis")
    public ResponseEntity<KpiResponseDTO> getKpis(@RequestParam(defaultValue = "last24h") String range) {
        return ResponseEntity.ok(reportService.getKpis(range));
    }

    /**
     * GET /api/report/top-units?range=last7d
     */
    @GetMapping("/top-units")
    public ResponseEntity<List<Map<String, Object>>> getTopUnits(@RequestParam(defaultValue = "last7d") String range) {
        return ResponseEntity.ok(reportService.getTopUnits(range));
    }

    /**
     * Endpoint interno para registrar métricas manualmente en pruebas sin Kafka
     */
    @PostMapping("/events/simulate")
    public ResponseEntity<String> simulateEvent(
            @RequestParam Long reservationId,
            @RequestParam Long unitId,
            @RequestParam String status) {
        reportService.recordReservationEvent(reservationId, unitId, status);
        return ResponseEntity.ok("Métrica registrada con éxito");
    }
}