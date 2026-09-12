package com.duocuc.ms_andesstay_catalog.repository;

import com.duocuc.ms_andesstay_catalog.model.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {
}