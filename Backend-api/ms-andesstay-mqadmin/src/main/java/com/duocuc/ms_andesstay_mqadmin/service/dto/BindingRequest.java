package com.duocuc.ms_andesstay_mqadmin.service.dto;

/**
 * Petición interna para crear o eliminar un binding. La valida la capa REST (tarea 10).
 *
 * @param queue      cola destino
 * @param exchange   exchange origen
 * @param routingKey routing key que enlaza ambos
 */
public record BindingRequest(
        String queue,
        String exchange,
        String routingKey) {
}
