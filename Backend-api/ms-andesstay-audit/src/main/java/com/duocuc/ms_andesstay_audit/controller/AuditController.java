package com.duocuc.ms_andesstay_audit.controller;

import com.duocuc.ms_andesstay_audit.model.AuditLog;
import com.duocuc.ms_andesstay_audit.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    // GET /api/audit (read-only)[cite: 2]
    @GetMapping
    public ResponseEntity<List<AuditLog>> getTimeline() {
        return ResponseEntity.ok(auditService.getAllAuditLogs());
    }

    // GET /api/audit/actor/{actor} (Filtro por usuario)[cite: 2]
    @GetMapping("/actor/{actor}")
    public ResponseEntity<List<AuditLog>> getTimelineByActor(@PathVariable String actor) {
        return ResponseEntity.ok(auditService.getLogsByActor(actor));
    }
}