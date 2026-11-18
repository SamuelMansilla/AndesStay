package com.duocuc.ms_andesstay_report.repository;

import com.duocuc.ms_andesstay_report.model.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Inserta el id del evento de forma atómica. Si ya existe, PostgreSQL no lanza
 * excepción y devuelve 0 filas afectadas, lo que permite ignorar el duplicado
 * sin marcar la transacción como rollback-only.
 */
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {

    @Modifying
    @Query(value = """
            INSERT INTO report_processed_messages
                (message_id, topic, partition_number, offset_number, processed_at)
            VALUES (:messageId, :topic, :partition, :offset, now())
            ON CONFLICT (message_id) DO NOTHING
            """, nativeQuery = true)
    int markProcessed(@Param("messageId") String messageId,
                      @Param("topic") String topic,
                      @Param("partition") int partition,
                      @Param("offset") long offset);
}
