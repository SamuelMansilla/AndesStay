package com.duocuc.ms_andesstay_notify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Topología de mensajería del microservicio, centralizada en
 * {@code application.yml} bajo {@code andesstay.messaging.*}.
 *
 * <p>Cubre el <b>indicador 1 de la pauta</b>: ningún nombre de cola, exchange,
 * binding o routing key debe escribirse como literal en el código Java; todos
 * se inyectan desde aquí.</p>
 *
 * <p>La convención de nombres es {@code q.<dominio>.<acción>} para colas,
 * {@code ex.andesstay.<dominio>} para exchanges y {@code <dominio>.<acción>}
 * para routing keys (ver {@code context.md} §7).</p>
 */
@ConfigurationProperties(prefix = "andesstay.messaging")
public class MessagingProperties {

    /** Exchange principal en el que publican los productores (p. ej. {@code ex.andesstay.notifications}). */
    private String exchange;

    /** Dead Letter Exchange: destino de los mensajes rechazados o agotados (p. ej. {@code ex.andesstay.dlx}). */
    private String dlx;

    /** Dead Letter Queue compartida (p. ej. {@code q.andesstay.dlq}). */
    private String dlq;

    /** Routing key con la que las colas reenvían sus descartes al DLX. */
    private String dlqRoutingKey;

    /** Nombres de cola, uno por caso de uso. */
    private Queues queues = new Queues();

    /** Routing keys, una por caso de uso. */
    private RoutingKeys routingKeys = new RoutingKeys();

    /**
     * Nombres de las colas por caso de uso.
     */
    public static class Queues {

        /** Notificación por email (canal EMAIL). */
        private String notifyEmail;

        /** Notificación por SMS (canal SMS). */
        private String notifySms;

        /** Notificación push (canal PUSH). */
        private String notifyPush;

        /** Tickets de housekeeping generados en el CHECKOUT de una reserva. */
        private String housekeepingTickets;

        /** Generación de documentos: vouchers y facturas. */
        private String docsGenerate;

        public String getNotifyEmail() {
            return notifyEmail;
        }

        public void setNotifyEmail(String notifyEmail) {
            this.notifyEmail = notifyEmail;
        }

        public String getNotifySms() {
            return notifySms;
        }

        public void setNotifySms(String notifySms) {
            this.notifySms = notifySms;
        }

        public String getNotifyPush() {
            return notifyPush;
        }

        public void setNotifyPush(String notifyPush) {
            this.notifyPush = notifyPush;
        }

        public String getHousekeepingTickets() {
            return housekeepingTickets;
        }

        public void setHousekeepingTickets(String housekeepingTickets) {
            this.housekeepingTickets = housekeepingTickets;
        }

        public String getDocsGenerate() {
            return docsGenerate;
        }

        public void setDocsGenerate(String docsGenerate) {
            this.docsGenerate = docsGenerate;
        }
    }

    /**
     * Routing keys por caso de uso.
     */
    public static class RoutingKeys {

        /** Routing key para notificaciones por email. */
        private String notifyEmail;

        /** Routing key para notificaciones por SMS. */
        private String notifySms;

        /** Routing key para notificaciones push. */
        private String notifyPush;

        /** Routing key para tickets de housekeeping. */
        private String housekeepingTicket;

        /** Routing key para la generación de documentos. */
        private String docsGenerate;

        public String getNotifyEmail() {
            return notifyEmail;
        }

        public void setNotifyEmail(String notifyEmail) {
            this.notifyEmail = notifyEmail;
        }

        public String getNotifySms() {
            return notifySms;
        }

        public void setNotifySms(String notifySms) {
            this.notifySms = notifySms;
        }

        public String getNotifyPush() {
            return notifyPush;
        }

        public void setNotifyPush(String notifyPush) {
            this.notifyPush = notifyPush;
        }

        public String getHousekeepingTicket() {
            return housekeepingTicket;
        }

        public void setHousekeepingTicket(String housekeepingTicket) {
            this.housekeepingTicket = housekeepingTicket;
        }

        public String getDocsGenerate() {
            return docsGenerate;
        }

        public void setDocsGenerate(String docsGenerate) {
            this.docsGenerate = docsGenerate;
        }
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getDlx() {
        return dlx;
    }

    public void setDlx(String dlx) {
        this.dlx = dlx;
    }

    public String getDlq() {
        return dlq;
    }

    public void setDlq(String dlq) {
        this.dlq = dlq;
    }

    public String getDlqRoutingKey() {
        return dlqRoutingKey;
    }

    public void setDlqRoutingKey(String dlqRoutingKey) {
        this.dlqRoutingKey = dlqRoutingKey;
    }

    public Queues getQueues() {
        return queues;
    }

    public void setQueues(Queues queues) {
        this.queues = queues;
    }

    public RoutingKeys getRoutingKeys() {
        return routingKeys;
    }

    public void setRoutingKeys(RoutingKeys routingKeys) {
        this.routingKeys = routingKeys;
    }
}
