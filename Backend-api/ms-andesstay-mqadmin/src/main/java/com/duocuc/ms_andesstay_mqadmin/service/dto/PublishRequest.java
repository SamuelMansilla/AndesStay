package com.duocuc.ms_andesstay_mqadmin.service.dto;

/**
 * Petición interna para publicar un mensaje de prueba. La valida la capa REST;
 * aquí solo transporta los parámetros al {@code RabbitAdminService}.
 *
 * @param exchange   exchange administrado de destino ({@code null} si el destino es una cola)
 * @param queue      cola administrada de destino (viaja por el exchange por defecto;
 *                   {@code null} si el destino es un exchange)
 * @param routingKey routing key ({@code null} = cadena vacía AMQP)
 * @param payload    cuerpo del mensaje (texto JSON)
 */
public record PublishRequest(
        String exchange,
        String routingKey,
        String queue,
        String payload) {
}
