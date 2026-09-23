package com.duocuc.ms_andesstay_report.dto;

import java.util.List;
import java.util.Map;

public class KpiResponseDTO {
    private String timeRange;
    private long totalEvents;
    private List<Map<String, Object>> statusBreakdown;
    private double activeOccupancyRate;
    private long reservationsToday;
    private double averageCycleHours;
    private long totalReservations;

    public KpiResponseDTO() {
    }

    public KpiResponseDTO(String timeRange, long totalEvents, List<Map<String, Object>> statusBreakdown, double activeOccupancyRate) {
        this.timeRange = timeRange;
        this.totalEvents = totalEvents;
        this.statusBreakdown = statusBreakdown;
        this.activeOccupancyRate = activeOccupancyRate;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(long totalEvents) {
        this.totalEvents = totalEvents;
    }

    public List<Map<String, Object>> getStatusBreakdown() {
        return statusBreakdown;
    }

    public void setStatusBreakdown(List<Map<String, Object>> statusBreakdown) {
        this.statusBreakdown = statusBreakdown;
    }

    public double getActiveOccupancyRate() {
        return activeOccupancyRate;
    }

    public void setActiveOccupancyRate(double activeOccupancyRate) {
        this.activeOccupancyRate = activeOccupancyRate;
    }

    public long getReservationsToday() {
        return reservationsToday;
    }

    public void setReservationsToday(long reservationsToday) {
        this.reservationsToday = reservationsToday;
    }

    public double getAverageCycleHours() {
        return averageCycleHours;
    }

    public void setAverageCycleHours(double averageCycleHours) {
        this.averageCycleHours = averageCycleHours;
    }

    public long getTotalReservations() {
        return totalReservations;
    }

    public void setTotalReservations(long totalReservations) {
        this.totalReservations = totalReservations;
    }
}