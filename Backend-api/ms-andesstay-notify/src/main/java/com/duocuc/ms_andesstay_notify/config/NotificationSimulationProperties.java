package com.duocuc.ms_andesstay_notify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Gancho de <b>demostración</b> del camino de reintentos (tarea 05, indicador 5).
 *
 * <p>Permite forzar un fallo <i>recuperable</i> del proveedor de notificaciones
 * para destinatarios concretos, de modo que se pueda evidenciar el ciclo
 * <i>reintento → descarte a DLQ</i> sin depender de un proveedor real
 * (SMTP / pasarela SMS / FCM).</p>
 *
 * <p>En operación normal la lista está <b>vacía</b> y no altera nada; se activa
 * sólo durante la verificación:</p>
 * <pre>
 *   java -jar target/ms-andesstay-notify-*.jar \
 *        --andesstay.notify.simulation.fail-recipients=retry@andesstay.cl
 * </pre>
 */
@ConfigurationProperties(prefix = "andesstay.notify.simulation")
public class NotificationSimulationProperties {

    /** Destinatarios para los que el envío falla de forma transitoria. */
    private List<String> failRecipients = new ArrayList<>();

    /** ¿Debe fallar (de forma recuperable) el envío a este destinatario? */
    public boolean isFailing(String recipient) {
        return recipient != null
                && failRecipients.stream().anyMatch(candidate -> candidate.equalsIgnoreCase(recipient.trim()));
    }

    public List<String> getFailRecipients() {
        return failRecipients;
    }

    public void setFailRecipients(List<String> failRecipients) {
        this.failRecipients = failRecipients;
    }
}
