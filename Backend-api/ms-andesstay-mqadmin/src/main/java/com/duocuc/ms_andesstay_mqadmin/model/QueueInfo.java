package com.duocuc.ms_andesstay_mqadmin.model;

import java.util.Map;

/**
 * Vista de una cola administrada (declarada o creada vía la API).
 *
 * <p>El número de mensajes y de consumidores se consulta al broker en el momento
 * de listar; su valor es {@code null} cuando la cola está registrada pero todavía
 * no existe en el broker (por ejemplo, antes de que {@code notify} la declare).</p>
 *
 * @param name          nombre de la cola
 * @param durable       si sobrevive a un reinicio del broker
 * @param autoDelete    si se elimina al quedar sin consumidores
 * @param exclusive     si es de uso exclusivo de la conexión que la declara
 * @param messageCount  mensajes en la cola ({@code null} si no existe en el broker)
 * @param consumerCount consumidores conectados ({@code null} si no existe en el broker)
 * @param arguments     argumentos de declaración (p. ej. dead-lettering, TTL)
 */
public record QueueInfo(
        String name,
        boolean durable,
        boolean autoDelete,
        boolean exclusive,
        Integer messageCount,
        Integer consumerCount,
        Map<String, Object> arguments) {
}
