package com.duocuc.ms_andesstay_audit.repository;

import com.duocuc.ms_andesstay_audit.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    // Método útil para el filtro de trazabilidad mencionado en el caso
    List<AuditLog> findByActor(String actor);
}