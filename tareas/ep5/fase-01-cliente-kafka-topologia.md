# Fase 1 — Cliente Kafka y topología

> **Estado:** pendiente · **Indicadores:** 1 (12%), 7 (12%) · **Depende de:** 00 · **Est.:** 1,5 d

## Objetivo

Configurar el cliente Kafka de forma explícita y consistente en `reservations`, `report` y `audit`, y alinear el clúster de Compose con los tópicos y parámetros del plan.

## Pasos

1. **Compose** (`infra/kafka/compose.yml`):
   - `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`.
   - `KAFKA_MIN_INSYNC_REPLICAS=2`.
   - `KAFKA_LOG_RETENTION_CHECK_INTERVAL_MS=60000`.
   - Init de tópicos con `--config retention.ms`, `--config retention.bytes`, `--config cleanup.policy=delete`, `--config min.insync.replicas=2`.
   - Crear `reservations.events.DLT` y `audit.timeline.DLT`.
   - Reemplazar `sleep 30` por espera activa (`kafka-topics --list` en loop).
2. **Propiedades** (`application.yml` o `.properties`):
   - Bootstrap con los 3 brokers (`localhost:9092,localhost:9093,localhost:9094`).
   - Productor: serializers JSON, `acks=all`, `enable.idempotence=true`, `retries`, `delivery.timeout.ms`, `batch.size=32768`, `linger.ms=20`, `compression.type=snappy`, `max.in.flight=5`.
   - Consumidor: `ErrorHandlingDeserializer`, `JsonDeserializer`, grupo, `auto-offset-reset`, `enable-auto-commit=false`, `max.poll.records=100`, `session.timeout.ms=10000`, `heartbeat.interval.ms=3000`, concurrencia 3.
   - Nombres de tópicos y grupos **sin literales Java**: `KafkaTopicsProperties` / `KafkaConsumerProperties`.
3. **Clases de configuración:**
   - `reservations`: `config/KafkaProducerConfig.java` (`ProducerFactory<String, ReservationEventDTO>`, `KafkaTemplate`).
   - `report` y `audit`: `config/KafkaConsumerConfig.java` (`ConsumerFactory`, `ConcurrentKafkaListenerContainerFactory`, `DefaultErrorHandler`, `DeadLetterPublishingRecoverer`, `KafkaTemplate` de DLT).
4. Documentar cada propiedad con su motivo (tuning, durabilidad, retención).

## Definition of Done

- [ ] `application.yml`/`application.properties` contienen toda la configuración, sin valores mágicos en Java.
- [ ] Existen `KafkaProducerConfig`/`KafkaConsumerConfig` con factories y contenedores.
- [ ] El compose crea los 2 tópicos de negocio y los 2 DLT con 3P/RF3 y configs.
- [ ] `auto.create.topics` deshabilitado.
- [ ] Los servicios arrancan con Kafka encendido y apagado sin romper el contexto.

## Verificación

```powershell
docker compose -f infra/kafka/compose.yml up -d
# Kafka UI :9000 -> Topics: reservations.events, audit.timeline, *.DLT con 3 particiones y RF 3
cd Backend-api/ms-andesstay-reservations; ./mvnw.cmd -q test
cd ../ms-andesstay-report;            ./mvnw.cmd -q test
cd ../ms-andesstay-audit;             ./mvnw.cmd -q test
```

## Commit sugerido

```text
feat(kafka): configura cliente, tópicos y tuning base
```

## Rollback

`git revert <hash>` o `git checkout main -- infra/kafka/compose.yml Backend-api/ms-andesstay-{reservations,report,audit}/src/main/resources`.
