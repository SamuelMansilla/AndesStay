package cl.duoc.andesstay.ms_andesstay_reservations.config;

import cl.duoc.andesstay.ms_andesstay_reservations.dto.ReservationEventDTO;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente Kafka del productor de {@code reservations} (indicador 1 de la EP5).
 *
 * <p>Se declara explícitamente en vez de depender sólo de la autoconfiguración
 * para dejar visibles las decisiones de durabilidad y tuning de la unidad 3:</p>
 * <ul>
 *   <li>{@code acks=all} + {@code enable.idempotence=true}: no se pierden eventos
 *       ni se duplican por reintentos de red (3.3.1).</li>
 *   <li>{@code batch.size=32KB}, {@code linger.ms=20}, {@code compression=snappy}:
 *       throughput del productor (3.3.2).</li>
 *   <li>{@code JsonSerializer} sin headers de tipo: el contrato es el DTO, no el
 *       nombre de la clase Java.</li>
 * </ul>
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, ReservationEventDTO> reservationEventProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Durabilidad e idempotencia (3.3.1)
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120_000);

        // Tuning (3.3.2)
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32 * 1024);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 20);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        // El contrato JSON no incluye metadatos de clase
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, ReservationEventDTO> kafkaTemplate(
            ProducerFactory<String, ReservationEventDTO> reservationEventProducerFactory) {
        return new KafkaTemplate<>(reservationEventProducerFactory);
    }
}
