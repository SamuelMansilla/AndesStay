package com.duocuc.ms_andesstay_mqadmin.model;

/**
 * Vista de un exchange administrado (declarado o creado vía la API).
 *
 * @param name       nombre del exchange
 * @param type       tipo AMQP: {@code direct}, {@code topic}, {@code fanout} o {@code headers}
 * @param durable    si sobrevive a un reinicio del broker
 * @param autoDelete si se elimina al quedar sin bindings
 */
public record ExchangeInfo(
        String name,
        String type,
        boolean durable,
        boolean autoDelete) {
}
