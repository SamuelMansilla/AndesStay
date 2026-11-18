package cl.duoc.andesstay.ms_andesstay_reservations.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Topología y comportamiento de Kafka centralizados en
 * {@code application.properties} bajo {@code andesstay.kafka.*}.
 *
 * <p>Ningún nombre de tópico se escribe como literal en el código Java.</p>
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

        /** Tópico de eventos de ciclo de vida de reservas. */
        private String reservationsEvents;

        /** Tópico del timeline de auditoría. */
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

        /** Nº de consumidores por instancia (máximo = nº de particiones). */
        private int concurrency = 3;

        /** Retardo artificial por mensaje para demos de consumer lag (0 = sin retardo). */
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
