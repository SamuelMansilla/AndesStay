package com.duocuc.ms_andesstay_catalog.controller;

import com.duocuc.ms_andesstay_catalog.model.Unit;
import com.duocuc.ms_andesstay_catalog.service.UnitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final UnitService unitService;

    public CatalogController(UnitService unitService) {
        this.unitService = unitService;
    }

    // Endpoint para obtener todas las unidades[cite: 2]
    @GetMapping("/units")
    public ResponseEntity<List<Unit>> getAllUnits() {
        List<Unit> units = unitService.findAllUnits();
        return ResponseEntity.ok(units);
    }

    // Endpoint para crear una unidad[cite: 2]
    @PostMapping("/units")
    public ResponseEntity<Unit> createUnit(@RequestBody Unit unit) {
        Unit createdUnit = unitService.createUnit(unit);
        return new ResponseEntity<>(createdUnit, HttpStatus.CREATED);
    }

    // Endpoint para actualizar tarifa o disponibilidad[cite: 2]
    @PutMapping("/units/{id}")
    public ResponseEntity<Unit> updateUnit(@PathVariable Long id, @RequestBody Unit unitDetails) {
        try {
            Unit updatedUnit = unitService.updateUnit(id, unitDetails);
            return ResponseEntity.ok(updatedUnit);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}