package com.duocuc.ms_andesstay_kafkaadmin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class AlterConfigsRequest {

    /**
     * Configs a aplicar. Un valor {@code null} o vacío elimina la sobreescritura
     * y deja el valor por defecto del broker.
     */
    @NotEmpty(message = "Debe enviar al menos una configuración")
    @NotNull
    private Map<String, String> configs;

    public Map<String, String> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, String> configs) {
        this.configs = configs;
    }
}
