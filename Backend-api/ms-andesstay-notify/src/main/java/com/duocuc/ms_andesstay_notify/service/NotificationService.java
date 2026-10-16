package com.duocuc.ms_andesstay_notify.service;

import com.duocuc.ms_andesstay_notify.config.NotificationSimulationProperties;
import com.duocuc.ms_andesstay_notify.messaging.exception.NonRecoverableException;
import com.duocuc.ms_andesstay_notify.messaging.exception.RecoverableException;
import com.duocuc.ms_andesstay_notify.model.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio del dominio <b>NOTIFICATIONS</b>: el envío por cada canal.
 *
 * <p>No conoce RabbitMQ: los listeners son los que aplican el contrato de ACK y
 * los que traducen las excepciones de esta clase al rechazo del mensaje
 * (indicador 3 y 4).</p>
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationSimulationProperties simulation;

    public NotificationService(NotificationSimulationProperties simulation) {
        this.simulation = simulation;
    }

    /** Envía la notificación por correo electrónico. */
    public void sendEmail(NotificationEvent event) {
        requireValid("EMAIL", event);
        simulateIfConfigured("EMAIL", event);
        log.info("[EMAIL] Enviado a: {} para la reserva #{}. Contenido: {}",
                event.getRecipient(), event.getReservationId(), event.getBody());
    }

    /** Envía la notificación por mensaje de texto. */
    public void sendSms(NotificationEvent event) {
        requireValid("SMS", event);
        simulateIfConfigured("SMS", event);
        log.info("[SMS] Enviado a: {}. Mensaje: {}",
                event.getRecipient(), event.getBody());
    }

    /** Envía la notificación push a la aplicación. */
    public void sendPush(NotificationEvent event) {
        requireValid("PUSH", event);
        simulateIfConfigured("PUSH", event);
        log.info("[PUSH] Enviada alerta a: {}. Notificación: {}",
                event.getRecipient(), event.getBody());
    }

    /**
     * Simula un fallo transitorio del proveedor para los destinatarios indicados
     * en configuración (ver {@link NotificationSimulationProperties}); por
     * defecto la lista está vacía y no interviene.
     */
    private void simulateIfConfigured(String channel, NotificationEvent event) {
        if (simulation.isFailing(event.getRecipient())) {
            throw new RecoverableException("Fallo transitorio simulado del proveedor " + channel
                    + " para " + event.getRecipient());
        }
    }

    /**
     * Validación del payload: un evento incompleto o incoherente es un error
     * <b>no recuperable</b> (reintentarlo daría siempre el mismo resultado).
     */
    private void requireValid(String channel, NotificationEvent event) {
        if (event == null) {
            throw new NonRecoverableException("Payload nulo en " + channel);
        }
        if (isBlank(event.getRecipient())) {
            throw new NonRecoverableException("Payload sin destinatario (recipient) en " + channel);
        }
        if (isBlank(event.getBody())) {
            throw new NonRecoverableException("Payload sin contenido (body) en " + channel);
        }
        if (isBlank(event.getChannel()) || !channel.equalsIgnoreCase(event.getChannel().trim())) {
            throw new NonRecoverableException("Canal del mensaje ('" + event.getChannel()
                    + "') incoherente con la cola " + channel);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
