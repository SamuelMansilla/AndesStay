package com.duocuc.ms_andesstay_kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public class LoadTestRequest {

    @Min(value = 1, message = "El mínimo es 1 mensaje")
    @Max(value = 200_000, message = "El máximo es 200.000 mensajes")
    private int count = 1_000;

    @Pattern(regexp = ResourceNaming.TOPIC_PATTERN, message = ResourceNaming.TOPIC_MESSAGE)
    private String topic;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
