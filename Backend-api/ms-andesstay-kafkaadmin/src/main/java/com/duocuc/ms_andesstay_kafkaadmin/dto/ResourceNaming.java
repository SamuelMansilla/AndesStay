package com.duocuc.ms_andesstay_kafkaadmin.dto;

/**
 * Reglas de nombres y rangos compartidas por los DTOs y el validador de tópicos.
 */
public final class ResourceNaming {

    private ResourceNaming() {
    }

    /** Mismo patrón que usa el admin de RabbitMQ: sin prefijo interno ni vacíos. */
    public static final String TOPIC_PATTERN = "^(?!__|amq\\.)[A-Za-z0-9._-]{3,249}$";
    public static final String TOPIC_MESSAGE =
            "El nombre debe tener entre 3 y 249 caracteres [A-Za-z0-9._-] y no puede empezar con '__' ni 'amq.'";

    public static final String GROUP_PATTERN = "^[A-Za-z0-9._-]{1,249}$";
    public static final String GROUP_MESSAGE = "El groupId no es válido";
}
