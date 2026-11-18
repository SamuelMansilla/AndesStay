package com.duocuc.ms_andesstay_audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Topología y comportamiento de Kafka centralizados en
 * {@code application.properties} bajo {@code andesstay.kafka.*}.
 */
@ConfigurationProperties(prefix = "andesstay.kafka")
public class AndesStayKafkaProperties {

    private final Topics topics = new Topics();
    private final Consumer consumer = new Consumer();

    public Topics getTopics() {
        return topics;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public static class Topics {

        private String reservationsEvents;
        private String auditTimeline;

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
    }

    public static class Consumer {

        private int concurrency = 3;
        private long simulatedDelayMs = 0;

        public int getConcurrency() {
            return concurrency;
        }

        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }

        public long getSimulatedDelayMs() {
            return simulatedDelayMs;
        }

        public void setSimulatedDelayMs(long simulatedDelayMs) {
            this.simulatedDelayMs = simulatedDelayMs;
        }
    }
}
