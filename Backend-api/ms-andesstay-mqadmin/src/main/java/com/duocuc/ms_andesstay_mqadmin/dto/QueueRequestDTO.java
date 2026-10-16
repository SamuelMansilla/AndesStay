package com.duocuc.ms_andesstay_mqadmin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear una cola (indicador 8 de la pauta).
 *
 * <p>Valida que el nombre no esté vacío, respete el charset/longitud y no use el
 * prefijo reservado {@code amq.}. Los flags tienen valores por defecto razonables
 * ({@code durable=true}, {@code autoDelete=false}, {@code deadLetter=true}).</p>
 */
public class QueueRequestDTO {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    @Size(min = 3, max = 255, message = "El nombre debe tener entre 3 y 255 caracteres")
    @Pattern(regexp = ResourceNaming.NAME_PATTERN, message = ResourceNaming.NAME_MESSAGE)
    private String name;

    private Boolean durable = Boolean.TRUE;

    private Boolean autoDelete = Boolean.FALSE;

    @Min(value = 0, message = "El TTL no puede ser negativo")
    private Integer ttlMillis;

    private Boolean deadLetter = Boolean.TRUE;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Integer getTtlMillis() {
        return ttlMillis;
    }

    public void setTtlMillis(Integer ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    public Boolean getDeadLetter() {
        return deadLetter;
    }

    public void setDeadLetter(Boolean deadLetter) {
        this.deadLetter = deadLetter;
    }
}
