package com.duocuc.ms_andesstay_kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

public class CreateTopicRequest {

    @NotBlank(message = "El nombre del tópico es obligatorio")
    @Pattern(regexp = ResourceNaming.TOPIC_PATTERN, message = ResourceNaming.TOPIC_MESSAGE)
    private String name;

    @NotNull(message = "El número de particiones es obligatorio")
    @Min(value = 1, message = "Debe haber al menos 1 partición")
    @Max(value = 50, message = "El máximo soportado es 50 particiones")
    private Integer partitions;

    @NotNull(message = "El factor de replicación es obligatorio")
    @Min(value = 1, message = "El factor de replicación mínimo es 1")
    @Max(value = 3, message = "El factor de replicación máximo es 3")
    private Short replicationFactor;

    /** Configuraciones opcionales (retención, cleanup policy, min ISR, etc.). */
    private Map<String, String> configs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPartitions() {
        return partitions;
    }

    public void setPartitions(Integer partitions) {
        this.partitions = partitions;
    }

    public Short getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(Short replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public Map<String, String> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, String> configs) {
        this.configs = configs;
    }
}
