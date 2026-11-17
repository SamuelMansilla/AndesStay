package com.duocuc.ms_andesstay_kafkaadmin.controller;

import com.duocuc.ms_andesstay_kafkaadmin.dto.AlterConfigsRequest;
import com.duocuc.ms_andesstay_kafkaadmin.dto.CreateTopicRequest;
import com.duocuc.ms_andesstay_kafkaadmin.dto.IncreasePartitionsRequest;
import com.duocuc.ms_andesstay_kafkaadmin.dto.LoadTestRequest;
import com.duocuc.ms_andesstay_kafkaadmin.dto.PublishRequest;
import com.duocuc.ms_andesstay_kafkaadmin.dto.ResourceNaming;
import com.duocuc.ms_andesstay_kafkaadmin.model.ClusterInfo;
import com.duocuc.ms_andesstay_kafkaadmin.model.ConsumerGroupInfo;
import com.duocuc.ms_andesstay_kafkaadmin.model.DeclaredTopic;
import com.duocuc.ms_andesstay_kafkaadmin.model.LoadTestResult;
import com.duocuc.ms_andesstay_kafkaadmin.model.OperationResult;
import com.duocuc.ms_andesstay_kafkaadmin.model.TopicInfo;
import com.duocuc.ms_andesstay_kafkaadmin.service.KafkaAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST de administración de Kafka (indicadores 3 y 9 de la EP5).
 *
 * <p>Controlador delgado: valida la entrada y delega en
 * {@link KafkaAdminService}. No conoce {@code AdminClient}.</p>
 */
@RestController
@RequestMapping("/api/kafkaadmin")
@Validated
@PreAuthorize("hasRole('Admin')")
public class KafkaAdminController {

    private final KafkaAdminService kafkaAdminService;

    public KafkaAdminController(KafkaAdminService kafkaAdminService) {
        this.kafkaAdminService = kafkaAdminService;
    }

    // ── Clúster ───────────────────────────────────────────────────────────────

    @GetMapping("/cluster")
    public ClusterInfo cluster() {
        return kafkaAdminService.getClusterInfo();
    }

    // ── Tópicos ───────────────────────────────────────────────────────────────

    @GetMapping("/topics")
    public List<TopicInfo> listTopics() {
        return kafkaAdminService.listTopics();
    }

    @GetMapping("/topics/declared")
    public List<DeclaredTopic> declaredTopics() {
        return kafkaAdminService.listDeclaredTopics();
    }

    @GetMapping("/topics/{name}")
    public TopicInfo.TopicDetail describeTopic(
            @PathVariable @Pattern(regexp = ResourceNaming.TOPIC_PATTERN,
                    message = ResourceNaming.TOPIC_MESSAGE) String name) {
        return kafkaAdminService.describeTopic(name);
    }

    @PostMapping("/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public TopicInfo.TopicDetail createTopic(@Valid @RequestBody CreateTopicRequest request) {
        return kafkaAdminService.createTopic(request);
    }

    @PutMapping("/topics/{name}/configs")
    public TopicInfo.TopicDetail alterConfigs(
            @PathVariable @Pattern(regexp = ResourceNaming.TOPIC_PATTERN,
                    message = ResourceNaming.TOPIC_MESSAGE) String name,
            @Valid @RequestBody AlterConfigsRequest request) {
        return kafkaAdminService.alterConfigs(name, request);
    }

    @PostMapping("/topics/{name}/partitions")
    public TopicInfo.TopicDetail increasePartitions(
            @PathVariable @Pattern(regexp = ResourceNaming.TOPIC_PATTERN,
                    message = ResourceNaming.TOPIC_MESSAGE) String name,
            @Valid @RequestBody IncreasePartitionsRequest request) {
        return kafkaAdminService.increasePartitions(name, request);
    }

    @DeleteMapping("/topics/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTopic(
            @PathVariable @Pattern(regexp = ResourceNaming.TOPIC_PATTERN,
                    message = ResourceNaming.TOPIC_MESSAGE) String name) {
        kafkaAdminService.deleteTopic(name);
    }

    @PostMapping("/topics/{name}/publish")
    public OperationResult publish(
            @PathVariable @Pattern(regexp = ResourceNaming.TOPIC_PATTERN,
                    message = ResourceNaming.TOPIC_MESSAGE) String name,
            @Valid @RequestBody PublishRequest request) {
        request.setTopic(name);
        return kafkaAdminService.publishTestMessage(request);
    }

    // ── Consumer groups ───────────────────────────────────────────────────────

    @GetMapping("/consumer-groups")
    public List<ConsumerGroupInfo> listConsumerGroups() {
        return kafkaAdminService.listConsumerGroups();
    }

    @GetMapping("/consumer-groups/{groupId}")
    public ConsumerGroupInfo.ConsumerGroupDetail describeConsumerGroup(
            @PathVariable @Pattern(regexp = ResourceNaming.GROUP_PATTERN,
                    message = ResourceNaming.GROUP_MESSAGE) String groupId) {
        return kafkaAdminService.describeConsumerGroup(groupId);
    }

    @DeleteMapping("/consumer-groups/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConsumerGroup(
            @PathVariable @Pattern(regexp = ResourceNaming.GROUP_PATTERN,
                    message = ResourceNaming.GROUP_MESSAGE) String groupId) {
        kafkaAdminService.deleteConsumerGroup(groupId);
    }

    // ── Carga ─────────────────────────────────────────────────────────────────

    @PostMapping("/load-test")
    public LoadTestResult loadTest(@Valid @RequestBody LoadTestRequest request) {
        return kafkaAdminService.loadTest(request);
    }
}
