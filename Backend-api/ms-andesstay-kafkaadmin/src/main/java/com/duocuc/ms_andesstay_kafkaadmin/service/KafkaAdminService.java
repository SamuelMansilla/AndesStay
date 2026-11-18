package com.duocuc.ms_andesstay_kafkaadmin.service;

import com.duocuc.ms_andesstay_kafkaadmin.config.KafkaTopicsProperties;
import com.duocuc.ms_andesstay_kafkaadmin.dto.AlterConfigsRequest;
import com.duocuc.ms_andesstay_kafkaadmin.dto.CreateTopicRequest;
import com.duocuc.ms_andesstay_kafkaadmin.dto.IncreasePartitionsRequest;
import com.duocuc.ms_andesstay_kafkaadmin.dto.LoadTestRequest;
import com.duocuc.ms_andesstay_kafkaadmin.dto.PublishRequest;
import com.duocuc.ms_andesstay_kafkaadmin.exception.ResourceNotFoundException;
import com.duocuc.ms_andesstay_kafkaadmin.model.ClusterInfo;
import com.duocuc.ms_andesstay_kafkaadmin.model.ConsumerGroupInfo;
import com.duocuc.ms_andesstay_kafkaadmin.model.DeclaredTopic;
import com.duocuc.ms_andesstay_kafkaadmin.model.LoadTestResult;
import com.duocuc.ms_andesstay_kafkaadmin.model.OperationResult;
import com.duocuc.ms_andesstay_kafkaadmin.model.TopicInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Única puerta al {@link AdminClient} (indicadores 3 y 9 de la EP5).
 *
 * <p>El controlador no conoce la API de Kafka: sólo llama a métodos de alto
 * nivel. Los errores de Kafka se traducen a HTTP en
 * {@code KafkaAdminExceptionHandler}.</p>
 */
@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);
    private static final long TIMEOUT_SECONDS = 10;

    /** Whitelist de configuraciones que el admin permite tocar. */
    private static final Set<String> ALLOWED_CONFIGS = Set.of(
            "retention.ms", "retention.bytes", "cleanup.policy", "min.insync.replicas",
            "segment.bytes", "segment.ms", "compression.type", "max.message.bytes",
            "delete.retention.ms", "file.delete.delay.ms");

    private static final Set<String> ALLOWED_COMPRESSION = Set.of(
            "uncompressed", "snappy", "lz4", "gzip", "zstd", "producer");

    private static final Set<String> ALLOWED_CLEANUP = Set.of("delete", "compact", "compact,delete");

    private final AdminClient adminClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProperties kafkaTopicsProperties;
    private final ObjectMapper objectMapper;

    public KafkaAdminService(AdminClient adminClient,
                             KafkaTemplate<String, String> kafkaTemplate,
                             KafkaTopicsProperties kafkaTopicsProperties,
                             ObjectMapper objectMapper) {
        this.adminClient = adminClient;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTopicsProperties = kafkaTopicsProperties;
        this.objectMapper = objectMapper;
    }

    // ── Clúster ───────────────────────────────────────────────────────────────

    public ClusterInfo getClusterInfo() {
        try {
            String clusterId = adminClient.describeCluster().clusterId().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Node controller = adminClient.describeCluster().controller().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Collection<Node> nodes = adminClient.describeCluster().nodes().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            List<ClusterInfo.BrokerInfo> brokers = nodes.stream()
                    .map(n -> new ClusterInfo.BrokerInfo(n.id(), n.host(), n.port(), n.rack()))
                    .toList();

            return new ClusterInfo(clusterId, controller != null ? controller.id() : -1, brokers);
        } catch (Exception ex) {
            throw translate("No se pudo describir el clúster", ex);
        }
    }

    // ── Tópicos ───────────────────────────────────────────────────────────────

    public List<TopicInfo> listTopics() {
        try {
            Set<String> names = adminClient.listTopics().names().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return names.stream()
                    .sorted()
                    .map(name -> describeTopicSummary(name))
                    .toList();
        } catch (Exception ex) {
            throw translate("No se pudieron listar los tópicos", ex);
        }
    }

    public TopicInfo.TopicDetail describeTopic(String name) {
        try {
            TopicDescription description = describeTopicDescription(name);
            Map<String, String> configs = describeTopicConfigs(name);

            List<TopicInfo.PartitionDetail> partitions = description.partitions().stream()
                    .map(p -> new TopicInfo.PartitionDetail(
                            p.partition(),
                            p.leader() != null ? p.leader().id() : -1,
                            p.replicas().stream().map(Node::id).toList(),
                            p.isr().stream().map(Node::id).toList()))
                    .toList();

            return new TopicInfo.TopicDetail(
                    name,
                    description.partitions().size(),
                    replicationFactor(description),
                    configs,
                    partitions);
        } catch (Exception ex) {
            throw translate("No se pudo describir el tópico " + name, ex);
        }
    }

    public TopicInfo.TopicDetail createTopic(CreateTopicRequest request) {
        String name = request.getName();
        Map<String, String> configs = validateConfigs(request.getConfigs());

        NewTopic newTopic = new NewTopic(name, request.getPartitions(), request.getReplicationFactor())
                .configs(configs);

        try {
            adminClient.createTopics(List.of(newTopic)).all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("✅ [KAFKAADMIN] Tópico creado: {} ({} particiones, RF={})",
                    name, request.getPartitions(), request.getReplicationFactor());
            // La metadata del tópico recién creado puede tardar en propagarse.
            return describeTopicWithRetry(name);
        } catch (Exception ex) {
            throw translate("No se pudo crear el tópico " + name, ex);
        }
    }

    public TopicInfo.TopicDetail alterConfigs(String name, AlterConfigsRequest request) {
        Map<String, String> configs = validateConfigs(request.getConfigs());
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);

        List<AlterConfigOp> operations = configs.entrySet().stream()
                .map(entry -> new AlterConfigOp(
                        new ConfigEntry(entry.getKey(), entry.getValue()),
                        entry.getValue() == null || entry.getValue().isBlank()
                                ? AlterConfigOp.OpType.DELETE
                                : AlterConfigOp.OpType.SET))
                .toList();

        try {
            adminClient.incrementalAlterConfigs(Map.of(resource, operations))
                    .all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("✅ [KAFKAADMIN] Configs actualizadas en {}: {}", name, configs.keySet());
            return describeTopic(name);
        } catch (Exception ex) {
            throw translate("No se pudieron alterar las configs de " + name, ex);
        }
    }

    public TopicInfo.TopicDetail increasePartitions(String name, IncreasePartitionsRequest request) {
        int target = request.getTotalPartitions();
        int current = describeTopic(name).partitions();
        if (target <= current) {
            throw new IllegalArgumentException(
                    "El total de particiones (" + target + ") debe ser mayor al actual (" + current + ")");
        }

        try {
            adminClient.createPartitions(Map.of(name, NewPartitions.increaseTo(target)))
                    .all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("✅ [KAFKAADMIN] Particiones de {}: {} -> {}", name, current, target);
            return describeTopic(name);
        } catch (Exception ex) {
            throw translate("No se pudieron aumentar las particiones de " + name, ex);
        }
    }

    public void deleteTopic(String name) {
        boolean isDeclared = kafkaTopicsProperties.getDeclared().stream()
                .anyMatch(topic -> name.equals(topic.getName()));
        if (isDeclared) {
            throw new IllegalArgumentException(
                    "El tópico '" + name + "' es parte de la topología declarada y no se puede eliminar desde el admin");
        }
        try {
            adminClient.deleteTopics(List.of(name)).all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("🗑️ [KAFKAADMIN] Tópico eliminado: {}", name);
        } catch (Exception ex) {
            throw translate("No se pudo eliminar el tópico " + name, ex);
        }
    }

    public List<DeclaredTopic> listDeclaredTopics() {
        return kafkaTopicsProperties.getDeclared().stream()
                .map(cfg -> new DeclaredTopic(
                        cfg.getName(),
                        cfg.getPartitions(),
                        cfg.getReplicationFactor(),
                        cfg.getRetentionMs(),
                        cfg.getRetentionBytes(),
                        cfg.getCleanupPolicy(),
                        cfg.getMinInsyncReplicas()))
                .toList();
    }

    // ── Consumer groups ───────────────────────────────────────────────────────

    public List<ConsumerGroupInfo> listConsumerGroups() {
        try {
            Collection<ConsumerGroupListing> listings =
                    adminClient.listConsumerGroups().all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            List<ConsumerGroupInfo> result = new ArrayList<>();
            for (ConsumerGroupListing listing : listings) {
                try {
                    ConsumerGroupInfo.ConsumerGroupDetail detail =
                            describeConsumerGroup(listing.groupId());
                    result.add(new ConsumerGroupInfo(
                            detail.groupId(),
                            detail.state(),
                            detail.members().size(),
                            detail.totalLag()));
                } catch (RuntimeException ex) {
                    result.add(new ConsumerGroupInfo(listing.groupId(), "UNKNOWN", 0, 0));
                }
            }
            result.sort((a, b) -> a.groupId().compareTo(b.groupId()));
            return result;
        } catch (Exception ex) {
            throw translate("No se pudieron listar los consumer groups", ex);
        }
    }

    public ConsumerGroupInfo.ConsumerGroupDetail describeConsumerGroup(String groupId) {
        try {
            ConsumerGroupDescription description = adminClient
                    .describeConsumerGroups(List.of(groupId)).all()
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .get(groupId);

            Map<TopicPartition, OffsetAndMetadata> committed = adminClient
                    .listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata()
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            Set<TopicPartition> partitions = new LinkedHashSet<>(committed.keySet());
            description.members().forEach(member ->
                    partitions.addAll(member.assignment().topicPartitions()));

            Map<TopicPartition, OffsetSpec> latestSpecs = new LinkedHashMap<>();
            partitions.forEach(tp -> latestSpecs.put(tp, OffsetSpec.latest()));

            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
                    latestSpecs.isEmpty()
                            ? Map.of()
                            : adminClient.listOffsets(latestSpecs).all()
                                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            long totalLag = 0;
            List<ConsumerGroupInfo.PartitionLag> lagDetails = new ArrayList<>();
            for (TopicPartition tp : partitions) {
                long current = committed.containsKey(tp) ? committed.get(tp).offset() : 0L;
                long end = endOffsets.containsKey(tp) ? endOffsets.get(tp).offset() : current;
                long lag = Math.max(0L, end - current);
                totalLag += lag;
                lagDetails.add(new ConsumerGroupInfo.PartitionLag(tp.topic(), tp.partition(), current, end, lag));
            }

            List<ConsumerGroupInfo.MemberDetail> members = description.members().stream()
                    .map(member -> new ConsumerGroupInfo.MemberDetail(
                            member.consumerId(),
                            member.clientId(),
                            member.host(),
                            member.assignment().topicPartitions().stream()
                                    .map(tp -> tp.topic() + "-" + tp.partition())
                                    .sorted()
                                    .toList()))
                    .toList();

            return new ConsumerGroupInfo.ConsumerGroupDetail(
                    description.groupId(),
                    description.state() != null ? description.state().toString() : "UNKNOWN",
                    description.coordinator() != null ? description.coordinator().toString() : null,
                    members,
                    lagDetails,
                    totalLag);
        } catch (GroupIdNotFoundException ex) {
            throw new ResourceNotFoundException("No existe el consumer group '" + groupId + "'");
        } catch (Exception ex) {
            throw translate("No se pudo describir el consumer group " + groupId, ex);
        }
    }

    public void deleteConsumerGroup(String groupId) {
        ConsumerGroupInfo.ConsumerGroupDetail detail = describeConsumerGroup(groupId);
        if (!"EMPTY".equalsIgnoreCase(detail.state())) {
            throw new IllegalArgumentException(
                    "Sólo se pueden eliminar consumer groups inactivos (estado actual: " + detail.state() + ")");
        }
        try {
            adminClient.deleteConsumerGroups(List.of(groupId)).all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("🗑️ [KAFKAADMIN] Consumer group eliminado: {}", groupId);
        } catch (Exception ex) {
            throw translate("No se pudo eliminar el consumer group " + groupId, ex);
        }
    }

    // ── Publicación de prueba y carga ─────────────────────────────────────────

    public OperationResult publishTestMessage(PublishRequest request) {
        String key = request.getKey() != null ? request.getKey() : "test";
        try {
            kafkaTemplate.send(request.getTopic(), key, request.getPayload())
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return new OperationResult(
                    request.getTopic(),
                    key,
                    request.getPayload().getBytes(StandardCharsets.UTF_8).length);
        } catch (Exception ex) {
            throw translate("No se pudo publicar el mensaje de prueba", ex);
        }
    }

    /**
     * Genera carga sintética con el mismo contrato JSON del productor de reservas.
     * Sirve para demostrar consumer lag y rebalanceo (unidad 3, guías 3.3.1/3.3.2).
     */
    public LoadTestResult loadTest(LoadTestRequest request) {
        String topic = request.getTopic() != null && !request.getTopic().isBlank()
                ? request.getTopic()
                : kafkaTopicsProperties.getTopics().getReservationsEvents();

        int count = request.getCount();
        CountDownLatch latch = new CountDownLatch(count);
        long start = System.nanoTime();

        for (int i = 0; i < count; i++) {
            long reservationId = (i % 1000) + 1L;
            long unitId = (i % 20) + 1L;
            String eventId = UUID.randomUUID().toString();
            String timestamp = LocalDateTime.now().toString();

            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventId", eventId);
            event.put("eventType", "CARGA_TEST");
            event.put("type", "CARGA_TEST");
            event.put("timestamp", timestamp);
            event.put("createdAt", timestamp);
            event.put("traceId", "trace-" + eventId.substring(0, 8));
            event.put("correlationId", "corr-" + eventId.substring(0, 8));
            event.put("actor", "kafkaadmin-load-test");
            event.put("reservationId", reservationId);
            event.put("unitId", unitId);
            event.put("status", "CREADA");
            event.put("details", "Evento sintético de prueba de carga");

            try {
                String payload = objectMapper.writeValueAsString(event);
                kafkaTemplate.send(topic, String.valueOf(reservationId), payload)
                        .whenComplete((result, ex) -> latch.countDown());
            } catch (Exception ex) {
                latch.countDown();
                log.warn("⚠️ [KAFKAADMIN] No se pudo serializar un evento de carga: {}", ex.getMessage());
            }
        }

        try {
            if (!latch.await(120, TimeUnit.SECONDS)) {
                throw new IllegalStateException("La prueba de carga no terminó en 120s");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Prueba de carga interrumpida", ex);
        }

        long durationMs = Math.max(1L, (System.nanoTime() - start) / 1_000_000L);
        double throughput = count / (durationMs / 1000.0);
        log.info("🚀 [KAFKAADMIN] Load test: {} mensajes en {} ms ({} msg/s) a {}",
                count, durationMs, String.format("%.2f", throughput), topic);
        return new LoadTestResult(count, durationMs, throughput);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TopicInfo.TopicDetail describeTopicWithRetry(String name) {
        RuntimeException last = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                return describeTopic(name);
            } catch (ResourceNotFoundException ex) {
                last = ex;
                try {
                    Thread.sleep(250);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrumpido esperando metadata del tópico", interrupted);
                }
            }
        }
        throw last != null ? last : new ResourceNotFoundException("No se pudo describir el tópico " + name);
    }

    private TopicInfo describeTopicSummary(String name) {
        try {
            TopicDescription description = describeTopicDescription(name);
            return new TopicInfo(
                    name,
                    description.partitions().size(),
                    replicationFactor(description),
                    name.startsWith("__"),
                    describeTopicConfigs(name));
        } catch (Exception ex) {
            throw translate("No se pudo describir el tópico " + name, ex);
        }
    }

    private TopicDescription describeTopicDescription(String name) {
        try {
            return adminClient.describeTopics(List.of(name)).allTopicNames()
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .get(name);
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof UnknownTopicOrPartitionException) {
                throw new ResourceNotFoundException("No existe el tópico '" + name + "'");
            }
            throw translate("No se pudo describir el tópico " + name, ex);
        } catch (Exception ex) {
            throw translate("No se pudo describir el tópico " + name, ex);
        }
    }

    private Map<String, String> describeTopicConfigs(String name) {
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        try {
            Config config = adminClient.describeConfigs(List.of(resource)).all()
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .get(resource);
            Map<String, String> result = new LinkedHashMap<>();
            config.entries().forEach(entry -> result.put(entry.name(), entry.value()));
            return result;
        } catch (Exception ex) {
            throw translate("No se pudieron leer las configs de " + name, ex);
        }
    }

    private int replicationFactor(TopicDescription description) {
        return description.partitions().stream()
                .mapToInt(p -> p.replicas().size())
                .max()
                .orElse(0);
    }

    private Map<String, String> validateConfigs(Map<String, String> configs) {
        if (configs == null || configs.isEmpty()) {
            return Map.of();
        }

        Map<String, String> validated = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (!ALLOWED_CONFIGS.contains(key)) {
                throw new IllegalArgumentException(
                        "Config no permitida: '" + key + "'. Permitidas: " + ALLOWED_CONFIGS);
            }

            if (value == null || value.isBlank()) {
                validated.put(key, value);
                continue;
            }

            switch (key) {
                case "retention.ms", "retention.bytes", "min.insync.replicas",
                     "segment.bytes", "segment.ms", "max.message.bytes", "delete.retention.ms" ->
                        requireNonNegativeLong(key, value);
                case "cleanup.policy" -> {
                    if (!ALLOWED_CLEANUP.contains(value)) {
                        throw new IllegalArgumentException(
                                "cleanup.policy inválida: '" + value + "'. Permitidas: " + ALLOWED_CLEANUP);
                    }
                }
                case "compression.type" -> {
                    if (!ALLOWED_COMPRESSION.contains(value)) {
                        throw new IllegalArgumentException(
                                "compression.type inválido: '" + value + "'. Permitidos: " + ALLOWED_COMPRESSION);
                    }
                }
                default -> {
                    // file.delete.delay.ms: se valida como numérico no negativo
                    requireNonNegativeLong(key, value);
                }
            }
            validated.put(key, value);
        }
        return validated;
    }

    private void requireNonNegativeLong(String key, String value) {
        try {
            if (Long.parseLong(value) < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El valor de '" + key + "' debe ser un número entero >= 0");
        }
    }

    private RuntimeException translate(String message, Exception ex) {
        if (ex instanceof RuntimeException runtime && !(ex instanceof ExecutionException)) {
            return runtime;
        }
        Throwable cause = ex instanceof ExecutionException && ex.getCause() != null ? ex.getCause() : ex;
        if (cause instanceof ResourceNotFoundException rnf) {
            return rnf;
        }
        if (cause instanceof UnknownTopicOrPartitionException) {
            return new ResourceNotFoundException(message + ": tópico inexistente");
        }
        if (cause instanceof GroupIdNotFoundException) {
            return new ResourceNotFoundException(message + ": consumer group inexistente");
        }
        if (cause instanceof TimeoutException) {
            return new IllegalStateException(message + ": timeout del broker", cause);
        }
        return new IllegalStateException(message + ": " + cause.getMessage(), cause);
    }
}
