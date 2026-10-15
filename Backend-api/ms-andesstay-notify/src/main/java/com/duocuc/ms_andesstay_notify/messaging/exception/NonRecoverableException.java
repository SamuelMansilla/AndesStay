package com.duocuc.ms_andesstay_notify.messaging.exception;

/**
 * Error <b>no recuperable</b>: el mensaje nunca podrá procesarse tal como llegó
 * (payload inválido o incompleto, canal desconocido, destinatario incoherente).
 *
 * <p>No se reintenta: reintentar daría siempre el mismo resultado. El listener
 * lo registra en {@code ERROR} y lo descarta directamente a la DLQ.</p>
 */
public class NonRecoverableException extends RuntimeException {

    public NonRecoverableException(String message) {
        super(message);
    }

    public NonRecoverableException(String message, Throwable cause) {
        super(message, cause);
    }
}
