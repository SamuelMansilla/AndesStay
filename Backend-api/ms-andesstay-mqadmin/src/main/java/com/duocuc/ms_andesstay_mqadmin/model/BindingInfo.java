package com.duocuc.ms_andesstay_mqadmin.model;

/**
 * Vista de un binding administrado (declarado o creado vía la API).
 *
 * @param queue      cola destino del binding
 * @param exchange   exchange origen del binding
 * @param routingKey routing key que enlaza exchange y cola
 */
public record BindingInfo(
        String queue,
        String exchange,
        String routingKey) {
}
