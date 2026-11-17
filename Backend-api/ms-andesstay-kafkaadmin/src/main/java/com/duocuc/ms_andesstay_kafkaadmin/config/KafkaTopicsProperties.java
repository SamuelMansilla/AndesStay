package com.duocuc.ms_andesstay_kafkaadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Topología declarada de Kafka (indicador 7 de la EP5).
 *
 * <p>El admin expone estas definiciones en {@code GET /topics/declared} y las
 * protege contra borrado accidental. Los valores deben coincidir con
 * {@code infra/kafka/compose.yml}.</p>
 *
 * <p>Se usa una lista (no un mapa con claves que contienen puntos) para que el
 * binding de Spring Boot sea inequívoco.</p>
 */
@ConfigurationProperties(prefix = "andesstay.kafka")
public class KafkaTopicsProperties {

    private final Topics topics = new Topics();
    private final List<TopicDefinition> declared = new ArrayList<>();

    public Topics getTopics() {
        return topics;
    }

    public List<TopicDefinition> getDeclared() {
        return declared;
    }

    public static class Topics {

        private String reservationsEvents;
        private String auditTimeline;
        private String reportMetrics;

        public String getReservationsEvents() {
            return reservationsEvents;
        }

        public void setReservationsEvents(String reservationsEvents) {
            this.reservationsEvents = reservationsEvents;
        }

        public String getAuditTimeline() {
            return auditTimeline;
        }

        public void setAuditTimeline(String auditTimeline) {
            this.auditTimeline = auditTimeline;
        }

        public String getReportMetrics() {
            return reportMetrics;
        }

        public void setReportMetrics(String reportMetrics) {
            this.reportMetrics = reportMetrics;
        }
    }

    public static class TopicDefinition {

        private String name;
        private int partitions = 3;
        private short replicationFactor = 3;
        private long retentionMs;
        private long retentionBytes;
        private String cleanupPolicy = "delete";
        private int minInsyncReplicas = 2;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getPartitions() {
            return partitions;
        }

        public void setPartitions(int partitions) {
            this.partitions = partitions;
        }

        public short getReplicationFactor() {
            return replicationFactor;
        }

        public void setReplicationFactor(short replicationFactor) {
            this.replicationFactor = replicationFactor;
        }

        public long getRetentionMs() {
            return retentionMs;
        }

        public void setRetentionMs(long retentionMs) {
            this.retentionMs = retentionMs;
        }

        public long getRetentionBytes() {
            return retentionBytes;
        }

        public void setRetentionBytes(long retentionBytes) {
            this.retentionBytes = retentionBytes;
        }

        public String getCleanupPolicy() {
            return cleanupPolicy;
        }

        public void setCleanupPolicy(String cleanupPolicy) {
            this.cleanupPolicy = cleanupPolicy;
        }

        public int getMinInsyncReplicas() {
            return minInsyncReplicas;
        }

        public void setMinInsyncReplicas(int minInsyncReplicas) {
            this.minInsyncReplicas = minInsyncReplicas;
        }
    }
}
