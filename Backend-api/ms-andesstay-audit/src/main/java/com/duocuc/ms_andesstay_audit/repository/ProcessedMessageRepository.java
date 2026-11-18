package com.duocuc.ms_andesstay_audit.repository;

import com.duocuc.ms_andesstay_audit.model.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {

    @Modifying
    @Query(value = """
            INSERT INTO audit_processed_messages
                (message_id, topic, partition_number, offset_number, processed_at)
            VALUES (:messageId, :topic, :partition, :offset, now())
            ON CONFLICT (message_id) DO NOTHING
            """, nativeQuery = true)
    int markProcessed(@Param("messageId") String messageId,
                      @Param("topic") String topic,
                      @Param("partition") int partition,
                      @Param("offset") long offset);
}
