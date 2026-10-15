package com.duocuc.ms_andesstay_notify.messaging.exception;

/**
 * Error <b>recuperable</b>: el mensaje sí se puede procesar, pero ahora no
 * (proveedor caído, timeout, recurso temporalmente no disponible).
 *
 * <p>El listener lo reintenta un número acotado de veces
 * (ver {@code RabbitListenerConfig}) y, si el problema persiste, el mensaje se
 * descarta a la DLQ. Nunca se reencola indefinidamente.</p>
 */
public class RecoverableException extends RuntimeException {

    public RecoverableException(String message) {
        super(message);
    }

    public RecoverableException(String message, Throwable cause) {
        super(message, cause);
    }
}
