package com.duocuc.ms_andesstay_kafkaadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class PublishRequest {

    @NotBlank(message = "El tópico es obligatorio")
    @Pattern(regexp = ResourceNaming.TOPIC_PATTERN, message = ResourceNaming.TOPIC_MESSAGE)
    private String topic;

    private String key;

    @NotBlank(message = "El payload es obligatorio")
    private String payload;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
