package com.duocuc.ms_andesstay_kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class IncreasePartitionsRequest {

    @NotNull(message = "El total de particiones es obligatorio")
    @Min(value = 1, message = "Debe haber al menos 1 partición")
    @Max(value = 50, message = "El máximo soportado es 50 particiones")
    private Integer totalPartitions;

    public Integer getTotalPartitions() {
        return totalPartitions;
    }

    public void setTotalPartitions(Integer totalPartitions) {
        this.totalPartitions = totalPartitions;
    }
}
