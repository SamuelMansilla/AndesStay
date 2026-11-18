package com.duocuc.ms_andesstay_kafkaadmin.model;

import java.util.List;

public record ConsumerGroupInfo(
        String groupId,
        String state,
        int members,
        long totalLag) {

    public record MemberDetail(
            String memberId,
            String clientId,
            String host,
            List<String> assignment) {
    }

    public record PartitionLag(
            String topic,
            int partition,
            long currentOffset,
            long logEndOffset,
            long lag) {
    }

    public record ConsumerGroupDetail(
            String groupId,
            String state,
            String coordinator,
            List<MemberDetail> members,
            List<PartitionLag> partitions,
            long totalLag) {
    }
}
