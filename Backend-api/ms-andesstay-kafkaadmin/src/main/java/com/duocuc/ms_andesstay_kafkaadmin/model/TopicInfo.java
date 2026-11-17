package com.duocuc.ms_andesstay_kafkaadmin.model;

import java.util.List;
import java.util.Map;

public record TopicInfo(
        String name,
        int partitions,
        int replicationFactor,
        boolean internal,
        Map<String, String> configs) {

    public record PartitionDetail(
            int partition,
            int leader,
            List<Integer> replicas,
            List<Integer> isr) {
    }

    public record TopicDetail(
            String name,
            int partitions,
            int replicationFactor,
            Map<String, String> configs,
            List<PartitionDetail> partitionsDetail) {
    }
}
