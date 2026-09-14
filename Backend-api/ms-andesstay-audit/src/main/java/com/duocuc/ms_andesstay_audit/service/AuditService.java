package com.duocuc.ms_andesstay_audit.service;

import com.duocuc.ms_andesstay_audit.model.AuditLog;
import com.duocuc.ms_andesstay_audit.repository.AuditLogRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // Escucha los mensajes de Kafka en el tópico "audit.timeline"
    @KafkaListener(topics = "audit.timeline", groupId = "audit-group")
    public void consumeAuditMessage(String message) {
        System.out.println("Evento de auditoría recibido: " + message);
        
        // Aquí idealmente parsearías el mensaje JSON, por simplicidad guardamos el texto plano
        AuditLog log = new AuditLog(
                "EVENTO_KAFKA", 
                "SISTEMA", 
                LocalDateTime.now(), 
                message
        );
        auditLogRepository.save(log);
    }

    // Métodos de solo lectura para la API
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }
    
    public List<AuditLog> getLogsByActor(String actor) {
        return auditLogRepository.findByActor(actor);
    }
}