package com.duocuc.ms_andesstay_mqadmin.service.dto;

/**
 * Petición interna para crear un exchange. La valida la capa REST (tarea 10).
 *
 * @param name       nombre del exchange
 * @param type       tipo AMQP: {@code direct}, {@code topic}, {@code fanout} o {@code headers}
 * @param durable    si debe ser durable
 * @param autoDelete si debe auto-eliminarse al quedar sin bindings
 */
public record ExchangeRequest(
        String name,
        String type,
        boolean durable,
        boolean autoDelete) {
}
