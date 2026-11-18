# Streaming con Kafka — AndesStay (EP5)

Documentación de la integración de **Apache Kafka** en AndesStay: topología,
productores, consumidores, manejo de errores, idempotencia, consumer groups y
administración. Corresponde a la Evaluación Parcial N°5 (12%) y a las guías
3.1.1, 3.1.2, 3.2.1, 3.2.2, 3.3.1, 3.3.2 y 3.3.3.

- [Arquitectura](#arquitectura)
- [Tópicos y parámetros](#tópicos-y-parámetros)
- [Cliente Kafka](#cliente-kafka)
- [Productor](#productor)
- [Consumidores](#consumidores)
- [Errores, reintentos y DLT](#errores-reintentos-y-dlt)
- [Idempotencia](#idempotencia)
- [Consumer groups y escalamiento](#consumer-groups-y-escalamiento)
- [Microservicio administrador](#microservicio-administrador)
- [Runbook](#runbook)
- [Dónde vive cada cosa](#dónde-vive-cada-cosa)

---

## Arquitectura

```text
   reservations (productor)
        │  key = reservationId
        ├──▶ reservations.events (3P / RF3) ──▶ report   (andesstay-report-group, concurrencia 3)
        └──▶ audit.timeline      (3P / RF3) ──▶ audit    (audit-group, concurrencia 3)

   errores de consumo
        ├──▶ reservations.events.DLT
        └──▶ audit.timeline.DLT

   opcional (fase 7): reservations.events ──▶ Kafka Streams ──▶ report.metrics
```

La separación **streaming (Kafka)** vs **comandos (RabbitMQ)** se mantiene:
Kafka alimenta reportería/auditoría en tiempo real; RabbitMQ sigue manejando
notificaciones, tickets y documentos (EP3).

---

## Tópicos y parámetros

| Tópico | Particiones | RF | `min.insync.replicas` | Retención | `retention.bytes` | `cleanup.policy` |
|---|---:|---:|---:|---|---:|---|
| `reservations.events` | 3 | 3 | 2 | 7 días | 1 GB | `delete` |
| `audit.timeline` | 3 | 3 | 2 | 30 días | 5 GB | `delete` |
| `reservations.events.DLT` | 3 | 3 | 2 | 7 días | 512 MB | `delete` |
| `audit.timeline.DLT` | 3 | 3 | 2 | 7 días | 512 MB | `delete` |
| `report.metrics` (opcional) | 3 | 3 | 2 | 7 días | 1 GB | `delete` |

Los tópicos se crean explícitamente en
[`infra/kafka/compose.yml`](../infra/kafka/compose.yml) con
`auto.create.topics.enable=false`. La justificación de retención está en
[`PLAN_EP5_KAFKA.md`](PLAN_EP5_KAFKA.md#4-tópicos-y-parámetros-indicador-7):
7 días para eventos operativos y 30 días para auditoría.

---

## Cliente Kafka

### Productor (`ms-andesstay-reservations`)

Archivo: `config/KafkaProducerConfig.java` + `application.properties`.

| Propiedad | Valor |
|---|---|
| `bootstrap.servers` | `${KAFKA_BOOTSTRAP_SERVERS}` (3 brokers) |
| `key.serializer` | `StringSerializer` |
| `value.serializer` | `JsonSerializer` (sin type headers) |
| `acks` | `all` |
| `enable.idempotence` | `true` |
| `retries` | `2147483647` |
| `max.in.flight.requests.per.connection` | `5` |
| `delivery.timeout.ms` | `120000` |
| `batch.size` | `32768` |
| `linger.ms` | `20` |
| `compression.type` | `snappy` |

### Consumidores (`report` y `audit`)

Archivos: `config/KafkaConsumerConfig.java`, `config/KafkaValidationConfig.java`.

| Propiedad | Valor |
|---|---|
| `value.deserializer` | `ErrorHandlingDeserializer` + `JsonDeserializer<ReservationEventDTO>` |
| `spring.json.trusted.packages` | `cl.duoc.andesstay.*,com.duocuc.*` |
| `group.id` | `andesstay-report-group` / `audit-group` |
| `auto.offset.reset` | `earliest` |
| `enable.auto.commit` | `false` |
| `max.poll.records` | `100` |
| `session.timeout.ms` | `10000` |
| `heartbeat.interval.ms` | `3000` |
| Ack mode | `RECORD` |
| Concurrencia | `3` (`andesstay.kafka.consumer.concurrency`) |

La concurrencia 3 coincide con las 3 particiones: cada instancia levanta 3
consumidores y el grupo reparte una partición por consumidor.

---

## Productor

`event/ReservationEventPublisher.java`:

- Construye un `ReservationEventDTO` (record inmutable).
- Publica el **mismo evento** en `reservations.events` y `audit.timeline`.
- Key = `reservationId` → orden por reserva y afinidad de partición.
- Headers: `X-Event-Id`, `X-Trace-Id`, `X-Correlation-Id`.
- Callback `whenComplete`: `INFO` con tópico/partición/offset; `WARN` con
  `eventId` si falla.
- Si `KAFKA_ENABLED=false`, conserva el fallback HTTP de la EP3.

---

## Consumidores

- `report/messaging/ReservationEventListener` → `ReservationEventProcessor` →
  `ReportService` (proyección de KPIs).
- `audit/messaging/AuditEventListener` → `AuditService` (timeline append-only).
- El listener sólo adapta Kafka; no tiene lógica de negocio ni acceso a BD.
- `@Valid` sobre el DTO: `eventId`, `eventType` y `reservationId` son
  obligatorios; `unitId` debe ser positivo.

---

## Errores, reintentos y DLT

`KafkaConsumerConfig` define:

1. **Deserialización tolerante:** `ErrorHandlingDeserializer`; un JSON corrupto
   no reinicia el contenedor en bucle.
2. **Reintentos:** `DefaultErrorHandler` con `ExponentialBackOffWithMaxRetries(2)`
   → 3 intentos (1 s → 2 s → 4 s).
3. **DLT:** `DeadLetterPublishingRecoverer` publica en `<topic>.DLT` con la misma
   partición y headers de excepción. `commitRecovered=true`.
4. **No reintentables:** `IllegalArgumentException`, `DeserializationException`,
   `SerializationException`, `ConstraintViolationException` y
   `MethodArgumentNotValidException` van directo al DLT.
5. **Logging contextual:** `RecordInterceptor` copia `eventId`/`traceId`/tópico/
   partición/offset a MDC; los logs incluyen esos campos.

La tabla de decisión es la misma que la unidad 3.3.3:

```text
éxito            -> ACK (commit del offset)
error transitorio -> reintento con backoff -> si se agota -> DLT
payload inválido  -> DLT inmediato
JSON corrupto     -> DLT inmediato (byte[] preservado)
```

---

## Idempotencia

Kafka garantiza *at-least-once*. Cada consumidor registra el `eventId` en una
tabla propia antes de aplicar el efecto de negocio:

- `report_processed_messages` (report).
- `audit_processed_messages` (audit).

El `INSERT ... ON CONFLICT (message_id) DO NOTHING` es atómico: si devuelve 0
filas, el evento ya fue procesado y se ignora. Las tablas son separadas porque
ambos servicios comparten la misma base Supabase y reciben el mismo `eventId`
desde tópicos distintos.

---

## Consumer groups y escalamiento

| Grupo | Tópico | Consumidor | Paralelismo |
|---|---|---|---|
| `andesstay-report-group` | `reservations.events` | report | 3 particiones / 3 hilos |
| `audit-group` | `audit.timeline` | audit | 3 particiones / 3 hilos |

- El paralelismo máximo es el número de particiones (3.1.2, 3.3.1).
- Para demostrar rebalanceo, levantar 2–3 instancias de `report` con el mismo
  `group-id` y puertos distintos; Kafka reparte las particiones.
- El **consumer lag** se consulta en `GET /api/kafkaadmin/consumer-groups/{id}`
  o en Kafka UI (`http://localhost:9000`).
- `andesstay.kafka.consumer.simulated-delay-ms` (default 0) permite simular un
  consumidor lento para la demo de lag.

---

## Microservicio administrador

`ms-andesstay-kafkaadmin` (puerto 8087), JWT + rol `Admin`, mismo patrón que
`mqadmin`. Endpoints bajo `/api/kafkaadmin`:

| Método | Ruta | Acción |
|---|---|---|
| `GET` | `/cluster` | Brokers, controller, cluster id |
| `GET` | `/topics` | Tópicos con particiones, RF y configs |
| `GET` | `/topics/declared` | Topología declarada (particiones, RF, retención, cleanup, min ISR) |
| `GET` | `/topics/{name}` | Detalle con líder, réplicas e ISR |
| `POST` | `/topics` | Crear tópico |
| `PUT` | `/topics/{name}/configs` | Alterar configs (whitelist) |
| `POST` | `/topics/{name}/partitions` | Aumentar particiones |
| `DELETE` | `/topics/{name}` | Eliminar tópico ad-hoc (protegido para los declarados) |
| `POST` | `/topics/{name}/publish` | Mensaje de prueba |
| `GET` | `/consumer-groups` | Grupos, estado, miembros y lag total |
| `GET` | `/consumer-groups/{id}` | Detalle, asignación y lag por partición |
| `DELETE` | `/consumer-groups/{id}` | Borrar grupo inactivo |
| `POST` | `/load-test` | Carga sintética (`count`, `topic`) |

Validación: nombre `^(?!__|amq\.)[A-Za-z0-9._-]{3,249}$`, particiones 1–50,
RF 1–3, whitelist de configs y valores numéricos/cleanup/compresión válidos.
Errores: 400 validación, 404 inexistente, 409 ya existe, 502 broker inalcanzable,
504 timeout.

---

## Runbook

```powershell
# 1) Clúster Kafka (3 ZK + 3 brokers + UI + topics init)
docker compose -f infra/kafka/compose.yml up -d
# UI: http://localhost:9000

# 2) Servicios (en cada carpeta)
$env:KAFKA_ENABLED="true"
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092,localhost:9093,localhost:9094"
.\mvnw.cmd spring-boot:run

# 3) Flujo normal
# POST /api/reservations -> reservations.events y audit.timeline
# GET  /api/report/kpis  -> proyección del consumidor
# GET  /api/audit        -> timeline

# 4) DLT (payload corrupto o incompleto)
docker exec kafka-1 bash -c "printf '2:no-es-json\n' | kafka-console-producer \
  --bootstrap-server localhost:29092 --topic reservations.events \
  --property parse.key=true --property key.separator=:"
# Ver reservations.events.DLT en Kafka UI

# 5) Carga y lag
# POST /api/kafkaadmin/load-test  {"count":100000,"topic":"reservations.events"}
# GET  /api/kafkaadmin/consumer-groups/andesstay-report-group
# Levantar 2-3 instancias de report para ver el rebalanceo

# 6) Admin
# GET  /api/kafkaadmin/topics
# GET  /api/kafkaadmin/topics/declared
# POST /api/kafkaadmin/topics  {"name":"demo.topic","partitions":3,"replicationFactor":3}
```

> En Docker, `KAFKA_BOOTSTRAP_SERVERS` debe apuntar a los advertised listeners
> del host (`host.docker.internal:9092/9093/9094` en Docker Desktop, o la IP
> pública en EC2). Los servicios fuera de Docker usan `localhost`.

### Verificación realizada contra Supabase (2026-11-17)

1. Conexión al pooler `:6543` con `sslmode=require` y `prepareThreshold=0`: OK.
2. Reserva #21 creada → evento en ambos tópicos → filas en `reservations`,
   `report_reservation_kpis`, `audit_logs`, `report_processed_messages` y
   `audit_processed_messages`.
3. Reserva #21 confirmada → segundo evento → estado `CONFIRMADA` en la
   proyección y segundo registro en el timeline.
4. Reenvío del mismo `eventId` → ambos consumidores registraron
   `Evento duplicado ignorado` y no se duplicaron filas.
5. Filtros de auditoría (`reservationId`, `actor`, `eventType` y combinación)
   corregidos y verificados contra Postgres (bug `lower(bytea)` por parámetros
   nulos).

---

## Dónde vive cada cosa

```text
Backend-api/ms-andesstay-reservations/
  config/KafkaProducerConfig.java
  config/AndesStayKafkaProperties.java
  dto/ReservationEventDTO.java
  event/ReservationEventPublisher.java

Backend-api/ms-andesstay-report/
  config/KafkaConsumerConfig.java
  config/KafkaValidationConfig.java
  config/AndesStayKafkaProperties.java
  dto/ReservationEventDTO.java
  messaging/ReservationEventListener.java
  service/ReservationEventProcessor.java
  model/ProcessedMessage.java
  repository/ProcessedMessageRepository.java

Backend-api/ms-andesstay-audit/
  config/KafkaConsumerConfig.java
  config/KafkaValidationConfig.java
  config/AndesStayKafkaProperties.java
  dto/ReservationEventDTO.java
  messaging/AuditEventListener.java
  service/AuditService.java
  model/ProcessedMessage.java
  repository/ProcessedMessageRepository.java

Backend-api/ms-andesstay-kafkaadmin/
  controller/KafkaAdminController.java
  service/KafkaAdminService.java
  config/KafkaAdminConfig.java
  config/KafkaTopicsProperties.java
  dto/*.java
  exception/KafkaAdminExceptionHandler.java
  model/*.java

Frontend/src/pages/StreamingAdminPage.tsx
Frontend/src/api/kafkaAdminService.ts
```
