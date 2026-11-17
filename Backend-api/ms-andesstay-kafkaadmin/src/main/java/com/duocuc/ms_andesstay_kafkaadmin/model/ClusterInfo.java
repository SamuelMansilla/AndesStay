package com.duocuc.ms_andesstay_kafkaadmin.model;

import java.util.List;

public record ClusterInfo(String clusterId, int controllerId, List<BrokerInfo> brokers) {

    public record BrokerInfo(int id, String host, int port, String rack) {
    }
}
