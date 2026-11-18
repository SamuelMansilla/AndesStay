package com.duocuc.ms_andesstay_kafkaadmin.model;

public record LoadTestResult(int sent, long durationMs, double throughputPerSecond) {
}
