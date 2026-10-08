package com.duocuc.ms_andesstay_audit.controller;

import com.duocuc.ms_andesstay_audit.model.AuditLog;
import com.duocuc.ms_andesstay_audit.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    // GET /api/audit (read-only con soporte para filtros de actor, tipo de evento y reserva)
    @GetMapping
    public ResponseEntity<List<AuditLog>> getTimeline(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Long reservationId) {
        return ResponseEntity.ok(auditService.getLogsFiltered(actor, eventType, reservationId));
    }

    // GET /api/audit/actor/{actor} (Filtro por usuario)
    @GetMapping("/actor/{actor}")
    public ResponseEntity<List<AuditLog>> getTimelineByActor(@PathVariable String actor) {
        return ResponseEntity.ok(auditService.getLogsByActor(actor));
    }

    // GET /api/audit/reservation/{reservationId} (Filtro por reserva)
    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<List<AuditLog>> getTimelineByReservation(@PathVariable Long reservationId) {
        return ResponseEntity.ok(auditService.getLogsFiltered(null, null, reservationId));
    }

    // POST /api/audit (Registro directo de evento de auditoría para integración / fallback)
    @PostMapping
    public ResponseEntity<AuditLog> createAuditLog(
            @RequestParam String eventType,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) Long reservationId,
            @RequestParam(required = false) String details,
            @RequestBody(required = false) String payload) {
        AuditLog saved = auditService.recordLog(eventType, actor, reservationId, details, payload);
        return ResponseEntity.ok(saved);
    }
}