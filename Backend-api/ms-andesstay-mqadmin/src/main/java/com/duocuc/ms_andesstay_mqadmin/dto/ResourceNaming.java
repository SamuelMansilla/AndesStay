package com.duocuc.ms_andesstay_mqadmin.dto;

/**
 * Reglas de nombrado compartidas por los DTOs de la API de administración.
 *
 * <p>Caracteres permitidos {@code [A-Za-z0-9._-]}, longitud 3–255 y prohibido el
 * prefijo reservado {@code amq.} (ver {@code context.md} §7). Se centralizan aquí
 * para no repetir el literal en cada anotación de validación.</p>
 */
public final class ResourceNaming {

    private ResourceNaming() {
    }

    /** Regex de nombre válido para colas y exchanges. */
    public static final String NAME_PATTERN = "^(?!amq\\.)[A-Za-z0-9._-]{3,255}$";

    /** Mensaje de error asociado a {@link #NAME_PATTERN}. */
    public static final String NAME_MESSAGE =
            "Nombre inválido: use de 3 a 255 caracteres [A-Za-z0-9._-] y no puede empezar por 'amq.'";

    /** Tipos de exchange AMQP soportados. */
    public static final String EXCHANGE_TYPE_PATTERN = "^(direct|topic|fanout|headers)$";
}
