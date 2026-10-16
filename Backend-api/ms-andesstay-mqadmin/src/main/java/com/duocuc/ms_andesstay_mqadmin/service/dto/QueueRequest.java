package com.duocuc.ms_andesstay_mqadmin.service.dto;

/**
 * Petición interna para crear una cola. La valida la capa REST (tarea 10); aquí
 * solo transporta los parámetros al {@code RabbitAdminService}.
 *
 * @param name       nombre de la cola
 * @param durable    si debe ser durable
 * @param autoDelete si debe auto-eliminarse al quedar sin consumidores
 * @param ttlMillis  TTL de los mensajes en milisegundos ({@code null} = sin TTL)
 * @param deadLetter si se configura dead-lettering hacia el DLX
 */
public record QueueRequest(
        String name,
        boolean durable,
        boolean autoDelete,
        Integer ttlMillis,
        boolean deadLetter) {
}
