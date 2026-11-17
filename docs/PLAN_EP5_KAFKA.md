# Plan EP5 — Streaming y analítica con Kafka (AndesStay · DSY1107)

> Documento de contexto y plan para la **Evaluación Parcial N°5** (12%).
> Fuente de requisitos: `EP5_DSY1107_Estudiante_encargo.pdf`.
> Contexto de unidad: guías 3.1.1, 3.1.2, 3.2.1, 3.2.2, 3.3.1, 3.3.2 y 3.3.3.
> Estado: **plan aprobado el 2026-11-17**; implementación por fases en [`../tareas/ep5/`](../tareas/ep5/README.md).

---

## 1. Objetivo

Incorporar **streaming de eventos con Apache Kafka** al sistema ya existente, sin romper las integraciones de la EP3 (RabbitMQ). La EP5 evalúa:

- configuración completa del cliente Kafka (brokers, serializers, consumer groups, propiedades avanzadas);
- productores y consumidores con **DTO tipados** sobre los dos tópicos del caso;
- microservicio **administrador de Kafka** (tópicos, configuraciones y consumer groups);
- manejo de errores (validación, logging, reintentos y **DLT**);
- parámetros de administración de tópicos (retención, cleanup policy, particiones, replicación);
- **consumer groups** para balanceo y resiliencia.

La unidad 3 aporta el marco: escalamiento por particiones, consumer lag, replicación, tuning (`batch.size`, `linger.ms`, `compression`), idempotencia, DLT y Kafka Streams.

---

## 2. Discrepancias y decisiones

| Tema | Discrepancia | Decisión |
|---|---|---|
| Frontend | El PDF dice Angular | Se mantiene **React 19**; el docente autorizó el cambio (ver [`../context.md`](../context.md:21)). Se documenta en la entrega. |
| RabbitMQ | La EP5 lo menciona indirectamente (compose, DLQ) | La EP3 queda **intacta**; solo se verifica que siga funcionando. |
| Tópicos | El PDF habla de “los dos tópicos” sin nombrarlos | Se mantienen los del caso: `reservations.events` y `audit.timeline`. |
| Admin | La rúbrica pide un microservicio administrador de Kafka | Se crea **`ms-andesstay-kafkaadmin`** (puerto 8087), dedicado y separado de `mqadmin`. |
| Kafka local | `KAFKA_ENABLED=false` por defecto | En el compose de demo se usa `KAFKA_ENABLED=true`; el fallback HTTP se conserva para desarrollo sin broker. |

---

## 3. Arquitectura de streaming

```text
   reservations (productor)
        │  key = reservationId
        ├──▶ reservations.events (3P / RF3) ──▶ report   (grupo andesstay-report-group, concurrencia 3)
        └──▶ audit.timeline      (3P / RF3) ──▶ audit    (grupo audit-group, concurrencia 3)

   errores de consumo (deserialización, validación, fallo de negocio)
        ├──▶ reservations.events.DLT
        └──▶ audit.timeline.DLT
             (DeadLetterPublishingRecoverer + DefaultErrorHandler)

   opcional (fase 7)
   reservations.events ──▶ Kafka Streams (report, 5 min) ──▶ report.metrics
```

- **Productor:** `ms-andesstay-reservations`. Publica el mismo `ReservationEventDTO` en ambos tópicos.
- **Consumidores:** `report` consume solo `reservations.events`; `audit` consume solo `audit.timeline`. Cada uno con su consumer group, de modo que no se duplican registros.
- **Idempotencia:** además, cada consumidor registra `eventId` en `processed_messages`; si el mismo evento se reentrega (at-least-once), se ignora. Es la red de seguridad de 3.3.3.
- **DLT:** los tópicos `.DLT` son creados por el compose y por el admin; no se consumen automáticamente para que se puedan inspeccionar. El admin ofrece replay opcional.

---

## 4. Tópicos y parámetros (indicador 7)

| Tópico | Particiones | RF | `min.insync.replicas` | Retención | `retention.bytes` | `cleanup.policy` | Uso |
|---|---:|---:|---:|---|---:|---|---|
| `reservations.events` | 3 | 3 | 2 | 7 días | 1 GB | `delete` | Eventos de ciclo de vida de reservas |
| `audit.timeline` | 3 | 3 | 2 | 30 días | 5 GB | `delete` | Timeline de auditoría |
| `reservations.events.DLT` | 3 | 3 | 2 | 7 días | 512 MB | `delete` | Fallos del consumidor de report |
| `audit.timeline.DLT` | 3 | 3 | 2 | 7 días | 512 MB | `delete` | Fallos del consumidor de audit |
| `report.metrics` (opcional) | 3 | 3 | 2 | 7 días | 1 GB | `delete` | Métricas de Kafka Streams |

**Reglas:** `auto.create.topics.enable=false`; los tópicos se crean explícitamente en [`../infra/kafka/compose.yml`](../infra/kafka/compose.yml) y/o desde el admin. `acks=all` + `min.insync.replicas=2` + RF=3 toleran la caída de un broker sin perder datos (3.3.1). La retención se justifica por flujo: 7 días para eventos operativos, 30 días para auditoría.

---

## 5. Configuración del cliente Kafka (indicador 1)

### Productor (`reservations`)

| Propiedad | Valor | Motivo |
|---|---|---|
| `spring.kafka.bootstrap-servers` | `${KAFKA_BOOTSTRAP_SERVERS:localhost:9092,localhost:9093,localhost:9094}` | Los 3 brokers del compose |
| `spring.kafka.producer.key-serializer` | `StringSerializer` | Key = `reservationId` |
| `spring.kafka.producer.value-serializer` | `JsonSerializer` | DTO tipado |
| `acks` | `all` | Durabilidad (3.3.1) |
| `enable.idempotence` | `true` | Evita duplicados de red |
| `retries` | `2147483647` | Reintentos acotados por `delivery.timeout.ms` |
| `max.in.flight.requests.per.connection` | `5` | Compatible con idempotencia |
| `delivery.timeout.ms` | `120000` | Tiempo máximo de entrega |
| `batch.size` | `32768` | Tuning (3.3.2) |
| `linger.ms` | `20` | Tuning (3.3.2) |
| `compression.type` | `snappy` | Throughput (3.3.2) |

### Consumidores (`report`, `audit`)

| Propiedad | Valor | Motivo |
|---|---|---|
| `value-deserializer` | `ErrorHandlingDeserializer` + `JsonDeserializer` | Un payload inválido no mata el contenedor (3.3.3) |
| `spring.json.trusted.packages` | `cl.duoc.andesstay.*,com.duocuc.*` | Solo DTOs del proyecto |
| `group-id` | `andesstay-report-group` / `audit-group` | Consumer groups separados |
| `auto-offset-reset` | `earliest` | No perder eventos en demo |
| `enable-auto-commit` | `false` | ACK explícito |
| `max.poll.records` | `100` | Control de lote |
| `session.timeout.ms` | `10000` | Rebalanceo razonable |
| `heartbeat.interval.ms` | `3000` | Idem |
| `concurrency` | `3` (configurable) | Paralelismo = nº de particiones (3.1.2, 3.3.1) |
| Ack mode | `RECORD` | Commit por registro procesado |

---

## 6. Manejo de errores (indicador 6)

1. **Validación de payload:** DTO con Bean Validation (`@NotNull`, `@NotBlank`, `@Positive`, `@Email`) y `@Valid` en el `@KafkaListener`; `Validator` configurado en el contenedor. Un payload inválido se rechaza antes de tocar la BD (3.3.3).
2. **Deserialización:** `ErrorHandlingDeserializer`; un JSON corrupto no reinicia el contenedor en bucle.
3. **Reintentos:** `DefaultErrorHandler` con `ExponentialBackOffWithMaxRetries(2)` → 3 intentos (1 s, 2 s, 4 s). Errores de negocio transitorios se reintentan; los no recuperables se pueden marcar con `AmqpRejectAndDontRequeueException` equivalente (`ImmediateAcknowledgeAmqpException` no aplica en Kafka; se usa una excepción no reintentable).
4. **DLT:** `DeadLetterPublishingRecoverer` publica en `<topic>.DLT` con la misma partición y agrega headers de excepción. `commitRecovered=true`.
5. **Idempotencia:** tabla `processed_messages` (`message_id` = `eventId` como PK, `topic`, `partition`, `offset`, `processed_at`); inserción y lógica de negocio en la misma transacción; duplicado → `skip`.
6. **Observabilidad:** headers `X-Event-Id`, `X-Trace-Id`, `X-Correlation-Id`; `RecordInterceptor` los copia a MDC; logs `INFO` de recibido, `WARN` de reintento y `ERROR` de DLT con tópico/partición/offset/key.
7. **Replay (opcional):** endpoint admin para reinyectar mensajes de un `.DLT` al tópico original (con límite y confirmación).

---

## 7. Microservicio administrador (indicadores 3 y 9)

`ms-andesstay-kafkaadmin` (puerto 8087), mismo patrón que `mqadmin`: Spring Web + Spring Kafka + Validation + OAuth2 Resource Server + Actuator, `@PreAuthorize("hasRole('Admin')")`, sin BD.

### Endpoints

| Método | Ruta | Acción |
|---|---|---|
| `GET` | `/api/kafkaadmin/cluster` | Brokers, controller, cluster id |
| `GET` | `/api/kafkaadmin/topics` | Tópicos con particiones, RF y configs |
| `GET` | `/api/kafkaadmin/topics/{name}` | Detalle de un tópico |
| `POST` | `/api/kafkaadmin/topics` | Crear tópico (`name`, `partitions`, `replicationFactor`, `configs`) |
| `PUT` | `/api/kafkaadmin/topics/{name}/configs` | Alterar configs (whitelist) |
| `POST` | `/api/kafkaadmin/topics/{name}/partitions` | Aumentar particiones |
| `DELETE` | `/api/kafkaadmin/topics/{name}` | Eliminar tópico (protegido para tópicos declarados) |
| `GET` | `/api/kafkaadmin/topics/declared` | Topología declarada en `application.yml` |
| `GET` | `/api/kafkaadmin/consumer-groups` | Grupos, estado, miembros y lag total |
| `GET` | `/api/kafkaadmin/consumer-groups/{groupId}` | Detalle, asignación y lag por partición |
| `DELETE` | `/api/kafkaadmin/consumer-groups/{groupId}` | Borrar grupo inactivo |
| `POST` | `/api/kafkaadmin/topics/{name}/publish` | Mensaje de prueba (demo) |
| `POST` | `/api/kafkaadmin/load-test?count=N` | Carga sintética para ver lag (demo) |
| `POST` | `/api/kafkaadmin/dlt/replay` | Replay acotado desde `.DLT` (opcional) |

### Validación y errores

- Nombre: `^(?!__|amq\.)[A-Za-z0-9._-]{3,249}$`.
- `partitions` 1–50; `replicationFactor` 1–3; configs con whitelist (`retention.ms`, `retention.bytes`, `cleanup.policy`, `min.insync.replicas`, `segment.bytes`, `compression.type`, `max.message.bytes`).
- 400 validación, 404 tópico/grupo inexistente, 409 ya existe / conflicto, 502 broker inalcanzable, 504 timeout.

---

## 8. Frontend (apoyo a la demo)

- Ruta `/streaming` (solo `Admin`), enlace en Navbar.
- Tabla de tópicos (particiones, RF, configs), crear/eliminar, editar retención/cleanup.
- Tabla de consumer groups con estado, miembros y **lag**.
- Botón “generar carga” para demostrar lag y rebalanceo.
- Reutiliza `axiosClient`, `ProtectedRoute` y el design system existente.

---

## 9. Fases

| Fase | Contenido | Indicadores | Est. |
|---|---|---|---|
| 0 | Preparación, rama, este plan y `tareas/ep5/` | — | 0,5 d |
| 1 | Compose Kafka + `KafkaConfig`/propiedades + tuning | 1, 7 | 1,5 d |
| 2 | `ReservationEventDTO` + productor tipado | 2, 4 | 1 d |
| 3 | Consumidores, validación, idempotencia, DLT, MDC | 5, 6 | 2 d |
| 4 | `ms-andesstay-kafkaadmin` | 3, 9 | 2 d |
| 5 | BFF, frontend `/streaming`, compose de apps | 3, 9 | 1,5 d |
| 6 | Carga, lag, retención y rebalanceo | 8 | 1 d |
| 7 | Kafka Streams (opcional) | valor agregado | 1,5 d |
| 8 | Pruebas, documentación y entrega | todas | 0,5 d |

Detalle y Definition of Done por fase en [`../tareas/ep5/README.md`](../tareas/ep5/README.md).

---

## 10. Cómo probar (runbook)

```bash
# 1) Clúster Kafka (3 ZK + 3 brokers + UI + topics init)
docker compose -f infra/kafka/compose.yml up -d
# UI: http://localhost:9000

# 2) Servicios con streaming habilitado (en cada carpeta)
$env:KAFKA_ENABLED="true"
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092,localhost:9093,localhost:9094"
.\mvnw.cmd spring-boot:run

# 3) Flujo
# POST /api/reservations -> evento en reservations.events y audit.timeline
# GET  /api/report/kpis  -> proyección del consumidor
# GET  /api/audit        -> timeline del consumidor
# Kafka UI -> Topics / Consumers (lag)

# 4) Errores y DLT
# POST /api/kafkaadmin/topics/reservations.events/publish con payload inválido
# Ver logs de reintento y mensaje en reservations.events.DLT

# 5) Escalamiento y lag
# POST /api/kafkaadmin/load-test?count=100000
# Levantar 2-3 instancias de report con el mismo group-id y ver el rebalanceo
```

---

## 11. Riesgos y observaciones

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Docker Desktop apagado | No se puede probar end-to-end | Levantarlo antes de F1/F6; tests unitarios mientras tanto |
| `KAFKA_ENABLED=false` por defecto | La demo no usa Kafka | `KAFKA_ENABLED=true` en el compose de apps y en el runbook |
| DTO duplicado en 3 servicios | Divergencia de contrato | Mismo JSON, tests de serialización y revisión conjunta; se evalúa módulo común solo si crece |
| Listeners externos en EC2 | El cliente no descubre brokers | `KAFKA_BOOTSTRAP_SERVERS` con los 3 advertised; `EC2_PUBLIC_IP` en compose |
| Tests embebidos en Windows | Lentitud/inestabilidad | Unit tests primero; integración con `@EmbeddedKafka` en perfil aparte |
| Credenciales versionadas (EP3) | Seguridad | Fuera del alcance EP5; se mantiene la observación de [`contexto.md`](contexto.md:295) |

---

## 12. Criterios de aceptación

- [ ] `reservations` publica DTO JSON en ambos tópicos con key `reservationId` y headers de trazabilidad.
- [ ] `report` y `audit` consumen con `@KafkaListener`, DTO tipado y consumer group propio.
- [ ] Payload inválido → validación → reintentos → `.DLT`, con logs contextuales.
- [ ] Idempotencia: un `eventId` repetido no duplica el efecto de negocio.
- [ ] `ms-andesstay-kafkaadmin` permite crear/listar/alterar/borrar tópicos y listar/describir/borrar consumer groups con lag.
- [ ] Tópicos con 3 particiones, RF=3, min ISR=2 y retención diferenciada.
- [ ] `/streaming` funciona con rol Admin.
- [ ] `mvnw test` y `npm run build` pasan.
- [ ] RabbitMQ (EP3) sigue funcionando.
