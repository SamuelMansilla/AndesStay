package com.duocuc.ms_andesstay_audit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Registro de idempotencia del consumidor de auditoría.
 *
 * <p>La tabla es propia de este servicio para no colisionar con la de reportería:
 * ambos consumen el mismo {@code eventId} desde tópicos distintos y comparten la
 * misma base de datos Supabase.</p>
 */
@Entity
@Table(name = "audit_processed_messages")
public class ProcessedMessage {

    @Id
    @Column(name = "message_id", length = 100, nullable = false)
    private String messageId;

    @Column(length = 255)
    private String topic;

    @Column(name = "partition_number")
    private int partition;

    @Column(name = "offset_number")
    private long offset;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public ProcessedMessage() {
    }

    public ProcessedMessage(String messageId, String topic, int partition, long offset, LocalDateTime processedAt) {
        this.messageId = messageId;
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.processedAt = processedAt;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getPartition() {
        return partition;
    }

    public void setPartition(int partition) {
        this.partition = partition;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
