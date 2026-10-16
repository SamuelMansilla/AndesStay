package com.duocuc.ms_andesstay_mqadmin.exception;

/**
 * Recurso (cola, exchange o binding) no administrado por este microservicio.
 *
 * <p>La lanza {@code RabbitAdminService} cuando se intenta operar sobre un recurso
 * que no está en su registro. El {@link MqAdminExceptionHandler} la traduce a un
 * {@code 404 Not Found} con un mensaje claro.</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
