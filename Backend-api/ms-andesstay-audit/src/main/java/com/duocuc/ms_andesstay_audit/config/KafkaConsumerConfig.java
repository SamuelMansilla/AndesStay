package com.duocuc.ms_andesstay_audit.config;

import com.duocuc.ms_andesstay_audit.dto.ReservationEventDTO;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cliente Kafka del consumidor de auditoría (EP5).
 *
 * <p>Misma política que reportería: DTO tipado, deserialización tolerante a
 * errores, reintentos con backoff y DLT, ACK por registro y MDC contextual.</p>
 */
@Configuration
public class KafkaConsumerConfig {

    private static final String[] TRUSTED_PACKAGES = {"cl.duoc.andesstay.*", "com.duocuc.*"};

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    private final AndesStayKafkaProperties kafkaProperties;

    public KafkaConsumerConfig(AndesStayKafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public ConsumerFactory<String, ReservationEventDTO> consumerFactory() {
        JsonDeserializer<ReservationEventDTO> jsonDeserializer =
                new JsonDeserializer<>(ReservationEventDTO.class, false);
        jsonDeserializer.addTrustedPackages(TRUSTED_PACKAGES);

        ErrorHandlingDeserializer<ReservationEventDTO> valueDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10_000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3_000);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReservationEventDTO> kafkaListenerContainerFactory(
            ConsumerFactory<String, ReservationEventDTO> consumerFactory,
            CommonErrorHandler kafkaErrorHandler,
            RecordInterceptor<String, ReservationEventDTO> mdcRecordInterceptor) {

        ConcurrentKafkaListenerContainerFactory<String, ReservationEventDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(kafkaProperties.getConsumer().getConcurrency());
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.setRecordInterceptor(mdcRecordInterceptor);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    @Bean
    public ProducerFactory<Object, Object> dltProducerFactory() {
        Map<Class<?>, org.apache.kafka.common.serialization.Serializer<?>> delegates = new LinkedHashMap<>();
        delegates.put(byte[].class, new ByteArraySerializer());
        delegates.put(Object.class, new JsonSerializer<>());
        DelegatingByTypeSerializer valueSerializer = new DelegatingByTypeSerializer(delegates, true);

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);

        @SuppressWarnings({"unchecked", "rawtypes"})
        DefaultKafkaProducerFactory factory = new DefaultKafkaProducerFactory<>(props);
        factory.setKeySerializer(new StringSerializer());
        factory.setValueSerializer(valueSerializer);
        return (ProducerFactory<Object, Object>) factory;
    }

    @Bean
    public KafkaTemplate<Object, Object> dltKafkaTemplate(
            ProducerFactory<Object, Object> dltProducerFactory) {
        return new KafkaTemplate<>(dltProducerFactory);
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(
            @Qualifier("dltKafkaTemplate") KafkaTemplate<Object, Object> dltKafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dltKafkaTemplate);

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(2);
        backOff.setInitialInterval(1_000);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(4_000);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.setCommitRecovered(true);
        handler.setLogLevel(KafkaException.Level.WARN);

        handler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                DeserializationException.class,
                SerializationException.class,
                jakarta.validation.ConstraintViolationException.class,
                org.springframework.web.bind.MethodArgumentNotValidException.class);

        return handler;
    }

    @Bean
    public RecordInterceptor<String, ReservationEventDTO> mdcRecordInterceptor() {
        return new RecordInterceptor<>() {
            @Override
            public ConsumerRecord<String, ReservationEventDTO> intercept(ConsumerRecord<String, ReservationEventDTO> record, org.apache.kafka.clients.consumer.Consumer<String, ReservationEventDTO> consumer) {
                MDC.clear();
                ReservationEventDTO event = record.value();
                if (event != null) {
                    MDC.put("eventId", event.eventId());
                    MDC.put("traceId", event.traceId());
                }
                MDC.put("topic", record.topic());
                MDC.put("partition", String.valueOf(record.partition()));
                MDC.put("offset", String.valueOf(record.offset()));
                return record;
            }

            @Override
            public void afterRecord(ConsumerRecord<String, ReservationEventDTO> record,
                                    org.apache.kafka.clients.consumer.Consumer<String, ReservationEventDTO> consumer) {
                MDC.clear();
            }
        };
    }
}
