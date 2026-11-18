package com.duocuc.ms_andesstay_kafkaadmin.model;

/**
 * Definición declarativa de un tópico (indicador 7): particiones, réplicas,
 * retención, cleanup policy y min.insync.replicas.
 */
public record DeclaredTopic(
        String name,
        int partitions,
        int replicationFactor,
        long retentionMs,
        long retentionBytes,
        String cleanupPolicy,
        int minInsyncReplicas) {
}
