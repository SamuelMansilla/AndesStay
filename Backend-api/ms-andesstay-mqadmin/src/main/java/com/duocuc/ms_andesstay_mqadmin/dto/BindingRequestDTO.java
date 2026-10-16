package com.duocuc.ms_andesstay_mqadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear (o eliminar) un binding (indicador 8 de la pauta).
 *
 * <p>Valida que la cola y el exchange no estén vacíos y respeten el nombrado de
 * recursos, y que la routing key no esté vacía.</p>
 */
public class BindingRequestDTO {

    @NotBlank(message = "La cola es obligatoria")
    @Pattern(regexp = ResourceNaming.NAME_PATTERN, message = ResourceNaming.NAME_MESSAGE)
    private String queue;

    @NotBlank(message = "El exchange es obligatorio")
    @Pattern(regexp = ResourceNaming.NAME_PATTERN, message = ResourceNaming.NAME_MESSAGE)
    private String exchange;

    @NotBlank(message = "La routing key es obligatoria")
    @Size(max = 255, message = "La routing key no puede superar 255 caracteres")
    private String routingKey;

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

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
}
