package com.duocuc.ms_andesstay_mqadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para publicar un mensaje de prueba hacia el broker.
 *
 * <p>Permite dos modos mutuamente excluyentes:</p>
 * <ul>
 *   <li><b>Exchange:</b> {@code exchange} (+ {@code routingKey}); el mensaje sale
 *       por el exchange administrado indicado y se enruta según sus bindings.</li>
 *   <li><b>Cola:</b> {@code queue}; el mensaje entra directo a la cola mediante el
 *       exchange por defecto ({@code ""}), ideal para verla crecer sin consumidores.</li>
 * </ul>
 * <p>La exclusividad se valida en {@code RabbitAdminService} (409 si falla).</p>
 */
public class PublishRequestDTO {

    /** Exchange administrado de destino (modo exchange). Opcional. */
    @Pattern(regexp = ResourceNaming.NAME_PATTERN, message = ResourceNaming.NAME_MESSAGE)
    private String exchange;

    /** Routing key del mensaje (vacía = cadena AMQP por defecto). */
    @Size(max = 255, message = "La routing key no puede superar 255 caracteres")
    private String routingKey;

    /** Cola administrada de destino (modo cola, vía exchange por defecto). Opcional. */
    @Pattern(regexp = ResourceNaming.NAME_PATTERN, message = ResourceNaming.NAME_MESSAGE)
    private String queue;

    /** Cuerpo del mensaje (texto plano; se envía como JSON). */
    @NotBlank(message = "El payload es obligatorio")
    @Size(max = 65536, message = "El payload no puede superar 64 KiB")
    private String payload;

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
