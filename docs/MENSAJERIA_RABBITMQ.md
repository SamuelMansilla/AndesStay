# Mensajería con RabbitMQ — AndesStay (EP3)

Documentación de la topología, la estrategia de errores y la API de administración
de RabbitMQ en AndesStay. Corresponde a las tareas 00–14 de la EP3.

- [Diagrama de topología](#diagrama-de-topología)
- [Convenciones de nombres](#convenciones-de-nombres)
- [Exchanges](#exchanges)
- [Colas](#colas)
- [Bindings](#bindings)
- [ACK / NACK, reintentos y DLQ](#ack--nack-reintentos-y-dlq)
- [Productores y consumidores](#productores-y-consumidores)
- [Microservicio de administración (`ms-andesstay-mqadmin`)](#microservicio-de-administración-ms-andesstay-mqadmin)
- [Guía de prueba end-to-end (incluye DLQ)](#guía-de-prueba-end-to-end-incluye-dlq)
- [Configuración y variables de entorno](#configuración-y-variables-de-entorno)
- [Dónde vive la topología en el código](#dónde-vive-la-topología-en-el-código)

---

## Diagrama de topología

```text
   PRODUCTORES                        EXCHANGE                         COLAS                         CONSUMIDORES
   ───────────                        ────────                         ─────                         ────────────

 reservations ──notify.email────────────────┐
 notify (API) ──notify.email────────────────┤
 notify (API) ──notify.sms──────────────────┤                                   ┌──▶ q.notify.email  ──▶ EmailListener              (notify)
 notify (API) ──notify.push─────────────────┼──▶ ex.andesstay.notifications ────┼──▶ q.notify.sms    ──▶ SmsListener               (notify)
                                            │        (direct, durable)          ├──▶ q.notify.push   ──▶ PushListener              (notify)
 reservations ──housekeeping.ticket─────────┤                                   ├──▶ q.housekeeping.tickets ──▶ HousekeepingTicketListener (notify)
 reservations ──docs.generate───────────────┘                                   └──▶ q.docs.generate ──▶ DocumentGenerationListener (notify)

   RECHAZO / REINTENTOS AGOTADOS
   ─────────────────────────────
   cualquiera de las 5 colas de negocio ──▶ ex.andesstay.dlx ──dlq.routing.key──▶ q.andesstay.dlq ──▶ DeadLetterListener (notify)
                                                (direct, durable)
```

Cada cola de negocio declara dead-lettering hacia `ex.andesstay.dlx`
(`x-dead-letter-exchange` + `x-dead-letter-routing-key`), de modo que un mensaje
rechazado sale de la cola original y llega a `q.andesstay.dlq`.

---

## Convenciones de nombres

| Elemento | Convención | Ejemplo |
|----------|------------|---------|
| Cola | `q.<dominio>.<acción>` | `q.notify.email`, `q.housekeeping.tickets` |
| Exchange | `ex.andesstay.<dominio>` | `ex.andesstay.notifications`, `ex.andesstay.dlx` |
| Routing key | `<dominio>.<acción>` | `notify.email`, `housekeeping.ticket` |
| Dead letter | `q.andesstay.dlq` + `dlq.routing.key` | — |

Ninguno de estos nombres aparece como literal en el código Java: todos se inyectan
desde `application.yml` mediante `MessagingProperties` (indicador 1).

---

## Exchanges

| Exchange | Tipo | Durable | Uso |
|----------|------|---------|-----|
| `ex.andesstay.notifications` | `direct` | sí | Exchange principal; recibe todas las notificaciones, tickets y solicitudes de documentos |
| `ex.andesstay.dlx` | `direct` | sí | Dead Letter Exchange; destino de los mensajes rechazados |

---

## Colas

| Cola | Durable | Dead-lettering | Consumidor (dominio) | Productor |
|------|---------|----------------|----------------------|-----------|
| `q.notify.email` | sí | → `ex.andesstay.dlx` | `EmailListener` (notify) | notify (API), reservations |
| `q.notify.sms` | sí | → `ex.andesstay.dlx` | `SmsListener` (notify) | notify (API) |
| `q.notify.push` | sí | → `ex.andesstay.dlx` | `PushListener` (notify) | notify (API) |
| `q.housekeeping.tickets` | sí | → `ex.andesstay.dlx` | `HousekeepingTicketListener` (notify) | reservations (al pasar a CHECKOUT) |
| `q.docs.generate` | sí | → `ex.andesstay.dlx` | `DocumentGenerationListener` (notify) | reservations (al crear/confirmar) |
| `q.andesstay.dlq` | sí | — | `DeadLetterListener` (notify) | todas, vía `ex.andesstay.dlx` |

---

## Bindings

| Exchange | Routing key | Cola destino |
|----------|-------------|--------------|
| `ex.andesstay.notifications` | `notify.email` | `q.notify.email` |
| `ex.andesstay.notifications` | `notify.sms` | `q.notify.sms` |
| `ex.andesstay.notifications` | `notify.push` | `q.notify.push` |
| `ex.andesstay.notifications` | `housekeeping.ticket` | `q.housekeeping.tickets` |
| `ex.andesstay.notifications` | `docs.generate` | `q.docs.generate` |
| `ex.andesstay.dlx` | `dlq.routing.key` | `q.andesstay.dlq` |

---

## ACK / NACK, reintentos y DLQ

Los consumidores usan **ACK manual** (`AcknowledgeMode.MANUAL`) mediante
`AbstractDlqListener`, con `defaultRequeueRejected=false`. El contrato de
confirmación es único para todos los dominios:

| Situación | Acción AMQP | Destino del mensaje |
|-----------|-------------|---------------------|
| `process(...)` termina bien | `basicAck` | confirmado y eliminado |
| `RecoverableException` (fallo transitorio) | **3 intentos** separados por 2 s y luego `basicNack(requeue=false)` | `q.andesstay.dlq` |
| `NonRecoverableException` u otro error inesperado | `basicNack(requeue=false)` (sin reintentos) | `q.andesstay.dlq` |

Decisiones de diseño:

- Los reintentos se ejecutan **dentro** del listener con `RetryTemplate` (política
  `SimpleRetryPolicy`: 3 intentos, sólo `RecoverableException`). No se usa el
  `RetryInterceptor` + `RepublishMessageRecoverer` del contenedor porque, en modo
  MANUAL, el `recoverer` republicaría mientras el ACK sigue pendiente del listener,
  generando duplicados.
- El descarte usa siempre `requeue=false`: la propia cola aplica su dead-lettering,
  así que el mensaje llega a la DLQ conservando su cuerpo y con los headers
  `x-death` (cola y motivo del rechazo).
- Nunca se reencola (`requeue=false`), por lo que no existe el bucle infinito
  clásico de `requeue=true`.
- Los reintentos se registran en `WARN` y el descarte en `ERROR`
  (`[REINTENTO]`, `[DESCARTE->DLQ]`), con canal, deliveryTag, excepción y payload.

---

## Productores y consumidores

- **Productores**
  - `ms-andesstay-reservations`: publica `housekeeping.ticket` (al pasar a CHECKOUT)
    y `docs.generate` (al crear/confirmar una reserva) usando
    `ReservationMessagingPublisher`.
  - `ms-andesstay-notify`: publica notificaciones por canal (`notify.email`,
    `notify.sms`, `notify.push`) vía su API REST.
  - Cada productor vive detrás de una clase `*Publisher` que encapsula
    `RabbitTemplate` y la routing key; los controladores/servicios no conocen AMQP.
- **Consumidores** (todos en `ms-andesstay-notify`)
  - `EmailListener`, `SmsListener`, `PushListener` → notificaciones.
  - `HousekeepingTicketListener` → tickets de aseo.
  - `DocumentGenerationListener` → generación de documentos.
  - `DeadLetterListener` → mensajes descartados en `q.andesstay.dlq`.

---

## Microservicio de administración (`ms-andesstay-mqadmin`)

Servicio Spring Boot en el **puerto 8086** que administra el broker (indicador 6).
Toda la lógica está encapsulada en `RabbitAdminService` (indicador 7); el
controlador sólo valida y delega. Requiere un **JWT válido con rol `Admin`**,
propagado por el BFF (`/api/mqadmin/**`).

### Endpoints

| Método | Ruta | Éxito | Descripción |
|--------|------|-------|-------------|
| `GET` | `/api/mqadmin/queues` | 200 | Lista colas administradas (con nº de mensajes y consumidores) |
| `POST` | `/api/mqadmin/queues` | 201 | Crea una cola |
| `DELETE` | `/api/mqadmin/queues/{name}` | 204 | Elimina una cola |
| `POST` | `/api/mqadmin/queues/{name}/purge` | 200 | Purga los mensajes de una cola |
| `GET` | `/api/mqadmin/exchanges` | 200 | Lista exchanges administrados |
| `POST` | `/api/mqadmin/exchanges` | 201 | Crea un exchange |
| `DELETE` | `/api/mqadmin/exchanges/{name}` | 204 | Elimina un exchange |
| `GET` | `/api/mqadmin/bindings` | 200 | Lista bindings |
| `POST` | `/api/mqadmin/bindings` | 201 | Crea un binding |
| `DELETE` | `/api/mqadmin/bindings` | 204 | Elimina un binding (identificado en el cuerpo) |

### Códigos de error

| Código | Cuándo |
|--------|--------|
| 400 | DTO inválido (nombre vacío, longitud/charset, prefijo `amq.`, tipo de exchange) — incluye `fields` por campo |
| 401 | Falta el JWT o es inválido |
| 403 | El JWT no tiene el rol `Admin` |
| 404 | El recurso no está administrado |
| 409 | Conflicto (por ejemplo, cola ya existente) |
| 502 | No se pudo contactar el broker RabbitMQ |

> **Nota:** Spring AMQP no expone un listado nativo completo del broker. El servicio
> mantiene un registro de recursos administrados, **sembrado** con la topología
> declarada en `andesstay.messaging.*` y **actualizado** con cada recurso creado o
> eliminado por la API. El conteo de mensajes sí se consulta al broker
> (`AmqpAdmin.getQueueInfo`).

### Ejemplos `curl`

```bash
# Token de Azure Entra ID con rol Admin (el mismo que usa el frontend)
TOKEN="<jwt>"

# Directo al microservicio
BASE="http://localhost:8086/api/mqadmin"

# A través del BFF (mismas rutas)
# BASE="http://localhost:8080/api/mqadmin"

# Listar colas
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/queues"

# Crear una cola con TTL y dead-lettering
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"q.prueba.demo","durable":true,"autoDelete":false,"deadLetter":true,"ttlMillis":60000}' \
  "$BASE/queues"

# Purgar la cola
curl -s -X POST -H "Authorization: Bearer $TOKEN" "$BASE/queues/q.prueba.demo/purge"

# Eliminar la cola
curl -s -X DELETE -H "Authorization: Bearer $TOKEN" "$BASE/queues/q.prueba.demo"

# Crear un exchange topic
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"ex.prueba.demo","type":"topic","durable":true}' "$BASE/exchanges"

# Crear un binding
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"queue":"q.prueba.demo","exchange":"ex.andesstay.notifications","routingKey":"prueba.demo"}' \
  "$BASE/bindings"
```

---

## Guía de prueba end-to-end (incluye DLQ)

> Requiere un broker RabbitMQ accesible. El cluster local está en
> `infra/mq/compose.yml`:
> ```bash
> docker compose -f infra/mq/compose.yml up -d
> ```
> Management UI: <http://localhost:15672> — usuario `andesstay`, contraseña `AndesStay2024!`, vhost `andesstay`.
> (El nodo secundario publica en 5673/15673 y **no** comparte topología si no está
> clusterizado; usar el nodo principal, 5672/15672.)

1. **Levantar** RabbitMQ, `notify`, `reservations`, `mqadmin` y `bff`
   (`infra/apps/compose.yml` o `mvnw spring-boot:run` por módulo).
2. **Reserva → documentos**: crear/confirmar una reserva y verificar que
   `q.docs.generate` recibe un mensaje (Management UI → Queues).
3. **CHECKOUT → housekeeping**: pasar una reserva a CHECKOUT y verificar
   `q.housekeeping.tickets`.
4. **Notificación normal**: enviar una notificación por la API de `notify`:
   ```bash
   curl -s -X POST -H "Content-Type: application/json" \
     -d '{"reservationId":1,"recipient":"huesped@andesstay.cl","channel":"EMAIL","subject":"Prueba","body":"Hola"}' \
     http://localhost:8083/api/notify/send
   ```
   El log de `notify` debe mostrar `[ACK] canal=EMAIL ...`.
5. **DLQ por error recuperable (reintentos)**: reiniciar `notify` simulando un
   proveedor que falla para un destinatario:
   ```bash
   # Ejecutar notify con el gancho de simulación activado
   java -jar target/ms-andesstay-notify-*.jar \
     --andesstay.notify.simulation.fail-recipients=retry@andesstay.cl
   ```
   Enviar una notificación a `retry@andesstay.cl`: el log mostrará 3 `[REINTENTO]`
   (con 2 s entre ellos) y luego `[DESCARTE->DLQ]`. El mensaje aparece en
   `q.andesstay.dlq`.
6. **DLQ por error no recuperable**: publicar un payload inválido (por ejemplo,
   `recipient` vacío) directamente al exchange con `routingKey=notify.email`
   (Management UI → Exchanges → Publish message). El mensaje va directo a
   `q.andesstay.dlq` sin reintentos.
7. **Administración desde la UI**: iniciar sesión como **Admin** → menú
   **Mensajería** (`/messaging`) → crear/purgar/eliminar colas, exchanges y bindings.
8. **Inspeccionar la DLQ**: Management UI → Queues → `q.andesstay.dlq` → *Get messages*
   (los headers `x-death` indican cola original y motivo).

---

## Configuración y variables de entorno

`notify`, `mqadmin` y `reservations` leen la conexión del broker de variables de
entorno (con *fallback* al cluster local):

| Variable | Default | Descripción |
|----------|---------|-------------|
| `RABBITMQ_HOST` | `localhost` | Host del broker |
| `RABBITMQ_PORT` | `5672` | Puerto AMQP |
| `RABBITMQ_USER` | `andesstay` | Usuario |
| `RABBITMQ_PASS` | `AndesStay2024!` | Contraseña |
| `RABBITMQ_VHOST` | `andesstay` | Virtual host |
| `AZURE_ISSUER_URI` | (tenant del IDaaS) | Issuer del JWT (`mqadmin`) |
| `AZURE_AUDIENCE` | (audience de la API) | Audience del JWT (`mqadmin`) |
| `NOTIFY_FAIL_RECIPIENTS` | vacío | Destinatarios con fallo transitorio simulado |

> **Riesgo R2 (secretos):** hoy las credenciales de RabbitMQ y Supabase tienen
> *fallback* en archivos versionados. Para producción deben externalizarse
> (variables de entorno / gestor de secretos) y rotarse. Ver `context.md` §10.

---

## Dónde vive la topología en el código

| Módulo | Archivo | Rol |
|--------|---------|-----|
| notify | `config/MessagingProperties.java` | Binding de `andesstay.messaging.*` (indicador 1) |
| notify | `config/messaging/*TopologyConfig.java`, `DeadLetterConfig.java` | Declaración de exchanges, colas y bindings |
| notify | `config/RabbitListenerConfig.java` | Factory de consumidores (MANUAL, sin requeue, reintentos) |
| notify | `messaging/AbstractDlqListener.java` | Contrato ACK/NACK/DLQ |
| notify | `messaging/<dominio>/*Publisher.java`, `*Listener.java` | Productores y consumidores por dominio |
| reservations | `config/MessagingProperties.java`, `messaging/ReservationMessagingPublisher.java` | Productor de tickets y documentos |
| mqadmin | `config/MessagingProperties.java` | Topología declarada (registro del admin) |
| mqadmin | `service/RabbitAdminService.java` | Única puerta a `AmqpAdmin` (indicador 7) |
| mqadmin | `controller/MqAdminController.java` | API REST de administración (indicador 6) |
| mqadmin | `config/SecurityConfig.java` | Resource server JWT + rol Admin |
| Frontend | `src/pages/MessagesAdminPage.tsx`, `src/api/mqAdminService.ts` | Panel de mensajería |
