package com.duocuc.ms_andesstay_catalog.service;

import com.duocuc.ms_andesstay_catalog.model.Unit;
import com.duocuc.ms_andesstay_catalog.repository.UnitRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UnitService {

    private final UnitRepository unitRepository;

    public UnitService(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    public List<Unit> findAllUnits() {
        return unitRepository.findAll();
    }

    public Unit createUnit(Unit unit) {
        return unitRepository.save(unit);
    }

    public Optional<Unit> getUnitById(Long id) {
        return unitRepository.findById(id);
    }

    public Unit updateUnit(Long id, Unit unitDetails) {
        return unitRepository.findById(id).map(existingUnit -> {
            // Actualizamos los campos necesarios (tarifa y disponibilidad según el caso)
            if (unitDetails.getPrice() != null) {
                existingUnit.setPrice(unitDetails.getPrice());
            }
            if (unitDetails.getAvailable() != null) {
                existingUnit.setAvailable(unitDetails.getAvailable());
            }
            if (unitDetails.getName() != null) {
                existingUnit.setName(unitDetails.getName());
            }
            if (unitDetails.getCapacity() != null) {
                existingUnit.setCapacity(unitDetails.getCapacity());
            }
            if (unitDetails.getType() != null) {
                existingUnit.setType(unitDetails.getType());
            }
            return unitRepository.save(existingUnit);
        }).orElseThrow(() -> new RuntimeException("Unidad no encontrada con ID: " + id));
    }
}