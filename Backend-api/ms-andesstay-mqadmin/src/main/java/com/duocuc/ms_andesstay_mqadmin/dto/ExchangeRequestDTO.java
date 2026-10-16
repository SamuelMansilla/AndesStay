package com.duocuc.ms_andesstay_mqadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear un exchange (indicador 8 de la pauta).
 *
 * <p>Valida el nombre (charset, longitud, sin prefijo {@code amq.}) y que el tipo
 * sea uno de los soportados por AMQP: {@code direct}, {@code topic},
 * {@code fanout} o {@code headers}.</p>
 */
public class ExchangeRequestDTO {

    @NotBlank(message = "El nombre del exchange es obligatorio")
    @Size(min = 3, max = 255, message = "El nombre debe tener entre 3 y 255 caracteres")
    @Pattern(regexp = ResourceNaming.NAME_PATTERN, message = ResourceNaming.NAME_MESSAGE)
    private String name;

    @NotBlank(message = "El tipo de exchange es obligatorio")
    @Pattern(regexp = ResourceNaming.EXCHANGE_TYPE_PATTERN,
            message = "Tipo inválido: use direct, topic, fanout o headers")
    private String type = "direct";

    private Boolean durable = Boolean.TRUE;

    private Boolean autoDelete = Boolean.FALSE;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getDurable() {
        return durable;
    }

    public void setDurable(Boolean durable) {
        this.durable = durable;
    }

    public Boolean getAutoDelete() {
        return autoDelete;
    }

    public void setAutoDelete(Boolean autoDelete) {
        this.autoDelete = autoDelete;
    }
}
