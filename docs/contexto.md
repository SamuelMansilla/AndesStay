# AndesStay — Contexto Técnico Integral

> Documento generado el **2026-10-22** a partir de la observación completa del código fuente (7 microservicios backend + frontend + infraestructura).
> Propósito: servir como mapa de referencia para retomar el desarrollo sobre el estado actual real del repositorio, complementando [`../context.md`](../context.md) (plan EP3) y [`INTEGRACION_FRONTEND_BACKEND.md`](INTEGRACION_FRONTEND_BACKEND.md) / [`MENSAJERIA_RABBITMQ.md`](MENSAJERIA_RABBITMQ.md).

---

## 1. Visión general del sistema

**AndesStay** es una plataforma de reservas para una red de hostales, cabañas y lodges. Arquitectura cloud-native:

```
                            ┌────────────────────────────┐
   Usuario                  │  Azure Entra ID (IDaaS)    │
      │                 ┌──▶│  MSAL / JWT / App Roles    │
      ▼                 │   └────────────────────────────┘
┌──────────────┐  redirect/login
│  Frontend    │──────────────────┐
│  React 19    │  (Axios + Bearer)│
│  Vite :5173  │                  ▼
└──────────────┘         ┌──────────────────┐    HTTP + JWT propagado
                         │  AWS API Gateway │──────┐
                         │  / BFF :8080     │      │
                         └──────────────────┘      ▼
        ┌─────────────┬─────────────┼─────────────┬──────────────┬─────────────┐
        ▼             ▼             ▼             ▼              ▼             ▼
  reservations   catalog       notify        report          audit        mqadmin
     :8081        :8082         :8083         :8084           :8085        :8086
  Kafka prod +   CRUD units   RabbitMQ      Kafka consumer  Kafka consum.  AmqpAdmin
  RabbitMQ prod  (JPA/Supabaseconsumidor +  (KPIs/Supabase) (timeline/     (RabbitMQ,
  (JPA/Supabase)              productor                     Supabase)       JWT + Admin)
        │             │                         │              │
        └─────────────┴────────────┬────────────┴──────────────┘
                                   ▼
                    Supabase PostgreSQL (pooler :6543)
        Mensajería: Kafka (streaming, deshabilitado por defecto)
                    RabbitMQ (comandos: notificaciones, tickets, documentos)
```

**División de responsabilidades de mensajería:**
- **Kafka** = streaming de eventos de reserva → alimentación de reportes (`report`) y auditoría (`audit`). Topics: `reservations.events`, `audit.timeline`. Requiere `KAFKA_ENABLED=true`; si no, hay **fallback HTTP** desde reservations.
- **RabbitMQ** = comandos/tareas asíncronas: notificaciones email/sms/push, tickets de housekeeping, generación de documentos (voucher/factura). Siempre activo local en `infra/mq/compose.yml`.

---

## 2. Mapa del repositorio

```
AndesStay/
├── README.md, context.md             ← plan EP3 (decisiones y pauta)
├── docs/
│   ├── INTEGRACION_FRONTEND_BACKEND.md   contratos, fixes JWT/MSAL, pooler Supabase
│   ├── MENSAJERIA_RABBITMQ.md            topología, ACK/DLQ, API mqadmin, pruebas E2E
│   └── contexto.md                       ← ESTE documento
├── Frontend/                         React 19 + TS + Vite + MSAL (SPA)
├── Backend-api/                      7 proyectos Maven independientes (cada uno con Dockerfile)
│   ├── ms-andesstay-bff/             :8080  puerta de enlace (Spring Security, valida JWT)
│   ├── ms-andesstay-reservations/    :8081  CRUD reservas + ciclo de vida + productores
│   ├── ms-andesstay-catalog/         :8082  unidades y tarifas (JPA)
│   ├── ms-andesstay-notify/          :8083  consumidores RabbitMQ + publishers
│   ├── ms-andesstay-report/          :8084  KPIs (consumer Kafka + fallback HTTP)
│   ├── ms-andesstay-audit/           :8085  timeline append-only (consumer Kafka)
│   └── ms-andesstay-mqadmin/         :8086  administración RabbitMQ (JWT + rol Admin)
├── infra/
│   ├── mq/       compose.yml          RabbitMQ 3.13 x2 nodos + rabbitmq.conf
│   ├── kafka/    compose.yml          ZK x3 + Kafka x3 + Kafka UI + topics-init
│   └── apps/     compose.yml + .env   los 7 servicios containerizados
└── tareas/                           plan de 15 tareas EP3 (todas completadas)
```

**Stack homogéneo del backend:** todos los microservicios usan **Spring Boot 3.3.4 + Java 21**, Maven (`mvnw.cmd`), multi-stage Docker (build `maven:3.9.6-eclipse-temurin-21`, runtime `eclipse-temurin:21-jdk-alpine`). Package base: `cl.duoc.andesstay.*` (bff, reservations) y `com.duocuc.*` (resto).

---

## 3. Frontend (React 19 + TypeScript + Vite)

### 3.1 Stack y arranque
- `Frontend/package.json`: React 19.2, React Router 7, Axios 1.20, `@azure/msal-browser` 5.21 + `@azure/msal-react` 5.7. Scripts: `dev` (`:5173`), `build` (`tsc -b && vite build`), `lint`.
- `vite.config.ts` mínimo (solo plugin react): **no hay proxy**; las llamadas van por URL absoluta.
- `Frontend/.env` (versionado): `VITE_AZURE_CLIENT_ID/TENANT_ID/REDIRECT_URI`, scope `api://.../OT.Create`, y `VITE_API_GATEWAY_URL` apuntando hoy a **`https://pn62ivjtk2.execute-api.us-east-1.amazonaws.com`** (comentadas: opción BFF local `http://localhost:8080`).

### 3.2 Autenticación (MSAL)
- `src/authConfig.ts`: crea `msalInstance` (`PublicClientApplication`), cache `localStorage`, `loginRequest` combina `User.Read` + `api://…/OT.Create` (decisión que evita el error MSAL `timed_out`).
- `src/main.tsx`: `initialize()` → `addEventCallback` (setea cuenta activa) → `handleRedirectPromise()` → envuelve la app en `<MsalProvider>`.

### 3.3 Cliente HTTP
- `src/api/axiosClient.ts`: `baseURL` = API Gateway (o BFF local). Interceptor de request: `acquireTokenSilent` (fallback popup) e **inyección `Authorization: Bearer`**; además decodifica y loguea los claims del JWT (debug). Interceptor de response: solo logging de errores.

### 3.4 Servicios API (`src/api/`)
| Servicio | Endpoints hacia el backend |
|---|---|
| `reservationsService` | `GET/POST /api/reservations`, `GET /{id}`, `PUT /{id}/status` |
| `catalogService` | `GET/POST /api/catalog/units`, `GET/PUT /{id}` |
| `reportService` | `GET /api/report/kpis?range`, `GET /api/report/top-units?range` |
| `auditService` | `GET /api/audit`, `GET /api/audit/actor/{actor}` |
| `mqAdminService` | CRUD `queues` (+`purge`), `exchanges`, `bindings`, `POST /publish` |

### 3.5 Roles y rutas
`src/types/index.ts` define `UserRole = Admin | Recepcionista | Huésped | Auditor`. `src/hooks/useUserRole.ts` lee `accounts[0].idTokenClaims.roles` (App Roles de Entra ID; alias `Operador`→Recepcionista, `Cliente`→Huésped; default `Huésped`). `src/components/ProtectedRoute.tsx` protege por rol (403 estilizado en pantalla).

| Ruta | Página | Roles |
|---|---|---|
| `/login` | LoginPage | pública |
| `/dashboard` | DashboardPage (adaptativo por rol + consola de prueba de conectividad) | autenticado |
| `/reservations` | ReservationsPage (tabla, filtro por estado, máquina de estados en cliente, modal de creación) | Admin, Recepcionista, Huésped |
| `/catalog` | CatalogPage (cards de unidades, precio CLP; edición solo Admin) | Admin, Recepcionista |
| `/reports` | ReportsPage (rango 24h/7d/30d, KPIs, Top Units) | Admin |
| `/audit` | AuditPage (timeline + filtros) | Admin, Auditor |
| `/messaging` | MessagesAdminPage (panel RabbitMQ: publish de prueba, CRUD colas/exchanges/bindings) | Admin |

- `Navbar.tsx`: links condicionales por rol, chip de usuario con badge de rol, logout redirect.
- Estilos: `src/styles/global.css` (design system propio con variables `--as-*`). **Sin estado global** (solo `useState/useEffect` por página; refresco manual).
- Legado sin usar: `useApi.ts`, `ProtectedData.tsx`, `index.css`, `App.css`.

---

## 4. Backend — Microservicios de dominio

### 4.1 `ms-andesstay-bff` (:8080) — Backend For Frontend

- **Rol:** gateway síncrono; no tiene BD (declara el driver postgres pero no usa JPA). Passthrough JSON puro.
- **SecurityConfig** (`config/SecurityConfig.java`): resource server JWT.
  - `JwtDecoder` con validadores: `JwtTimestampValidator` + **`TenantIssuerValidator`** (acepta issuer v1 `sts.windows.net` y v2 `login.microsoftonline.com` si contienen el tenantId `00d7b2a1-…`) + **`AudienceValidator`** flexible (con o sin `api://`; audience `api://130c7bbc-…`).
  - **`JwtRoleConverter`**: claim `roles` del JWT → `ROLE_*`.
  - Reglas: preflight OPTIONS permitAll → `GET /api/catalog/**` (y aliases `/api/units/**`, `/api/v1/**`) permitAll → `/actuator/**` permitAll → resto **autenticado**. 401 en JSON custom.
- **Controllers** (todos con `WebClient`, propagando `Authorization: Bearer <jwt>` y devolviendo el JSON crudo del downstream):
  | Controller | Ruta base | Destino | Endpoints |
  |---|---|---|---|
  | `BffReservationsController` | `/api/reservations` | :8081 | `GET` (filtros), `GET /{id}`, `POST`, `PUT /{id}/status` |
  | `BffCatalogController` | `/api/catalog`, `/api/v1/catalog`, `/api/units`, `/api/v1/units` (4 aliases) | :8082 | `GET /units`, `GET /units/{id}`, `POST`, `PUT /{id}` |
  | `BffReportController` | `/api/report` | :8084 | `GET /kpis?range`, `GET /top-units?range` |
  | `BffAuditController` | `/api/audit` | :8085 | `GET` (filtros), `GET /actor/{actor}`, `GET /reservation/{id}` |
  | `BffMqAdminController` | `/api/mqadmin` | :8086 | 12 endpoints de administración (no exige rol aquí, delega en mqadmin) |
- **`BffExceptionHandler`**: `WebClientRequestException` → **502** elucidativo; `WebClientResponseException` → reenvía status/body downstream.
- **Config:** `services.<ms>.url` con defaults `http://localhost:808x`; CORS restrictivo en `CorsConfigurationSource` (permite `localhost:5173`) aunque los controllers llevan `@CrossOrigin(origins="*")` (contradicción menor). Nota: hay variable para `notify` (:8083) pero no existe controller que lo proxyee — las notificaciones van por RabbitMQ.

### 4.2 `ms-andesstay-reservations` (:8081) — Núcleo de reservas

- **Dependencias:** data-jpa, validation, web, spring-kafka, spring-amqp. **Sin seguridad** (confía en la red interna; el BFF valida el JWT).
- **Endpoints** (`controller/ReservationController.java`, `/api/reservations`):
  | Método | Ruta | Entrada | Respuesta |
  |---|---|---|---|
  | POST | `/api/reservations` | `CreateReservationDTO` `@Valid` (`guestEmail` @Email @NotNull, `unitId` @NotNull, `checkInDate` @FutureOrPresent, `checkOutDate` @NotNull) | `201` + `Reservation` |
  | GET | `/api/reservations` | `?status` (enum), `?from`, `?to` | `List<Reservation>` |
  | GET | `/api/reservations/{id}` | — | `Reservation` (404 lógico) |
  | PUT | `/api/reservations/{id}/status` | `UpdateStatusDTO` (`status` @NotNull) | `Reservation` |

- **Máquina de estados** (enum `ReservationStatus`, acepta alias con tilde `EN_ESTADÍA`):
  ```
  CREADA ─▶ CONFIRMADA ─▶ CHECKIN_PENDIENTE ─▶ EN_ESTADÍA (check-in) ─▶ CHECKOUT
      └───────────────────────────▶ CANCELADA (desde cualquier estado)
  ```
  Regla clave (`ReservationService.updateStatus`): no se permite `EN_ESTADIA` salvo desde `CONFIRMADA` o `CHECKIN_PENDIENTE`. Además valida `checkOutDate > checkInDate` al crear.
- **Publicación de eventos (nunca bloquea el core):**
  - **Kafka** (`event/ReservationEventPublisher.java`): `KafkaTemplate` *opcional*, activada por `KAFKA_ENABLED`. Topics **`reservations.events`** y **`audit.timeline`**; key = reservationId. Envelope JSON estándar: `{eventId, eventType, type, timestamp, createdAt, traceId, correlationId, actor, reservationId, unitId, status, details}` (serializado con `String.format`, sin escape JSON estricto).
  - **Fallback HTTP** (si Kafka off): `java.net.http.HttpClient` async (timeouts 800ms/1200ms) → `POST {report}/api/report/events/simulate` y `POST {audit}/api/audit` con el mismo envelope; errores tragados.
  - **RabbitMQ** (`messaging/ReservationMessagingPublisher.java` + `config/RabbitMQConfig.java` + `MessagingProperties` `@ConfigurationProperties(prefix="andesstay.messaging")`): **solo productor**, JSON vía `Jackson2JsonMessageConverter`, publicación con `CompletableFuture.runAsync` y try/catch (WARN si el broker cae). No declara topología (dueño: `notify`). Ninguna routing key como literal Java.
    | Transición | Routing key | Cola | Payload |
    |---|---|---|---|
    | Reserva creada | `docs.generate` | `q.docs.generate` | `DocumentEvent` tipo **VOUCHER** |
    | Reserva CONFIRMADA | `docs.generate` | `q.docs.generate` | `DocumentEvent` tipo **FACTURA** |
    | Reserva CHECKOUT | `housekeeping.ticket` | `q.housekeeping.tickets` | `TicketEvent` tipo **LIMPIEZA** |
- **BD:** tabla `reservations` (`id`, `guest_email`, `unit_id`, `check_in_date`, `check_out_date`, `status`, `created_at`) — Supabase PostgreSQL pooler `aws-1-us-east-1.pooler.supabase.com:6543` (Transaction Mode, `prepareThreshold=0`), HikariCP `maximum-pool-size=3` (dimensionado para no saturar el pooler), `ddl-auto=update`.
- Repositorio: `findByStatus`, `findByGuestEmail`, `findByCheckInDateBetween`.
- Sin `@ControllerAdvice` (errores de negocio caen en 500 default: `IllegalArgumentException`/`IllegalStateException`).

### 4.3 `ms-andesstay-catalog` (:8082) — Unidades y tarifas

- **Dependencias:** web + data-jpa + postgresql. **Sin seguridad, sin RabbitMQ, sin Kafka.**
- **Endpoints** (`controller/CatalogController.java`, `/api/catalog`): `GET /units`, `POST /units` (`201`), `PUT /units/{id}` (actualización *patch-style* de campos no nulos; `RuntimeException` → 404). **No hay GET /{id} ni DELETE expuestos.**
- **Entidad `Unit`** (tabla `units`): `id`, `name`, `type` (HABITACION/CABAÑA), `capacity`, `price` (BigDecimal), `available` (Boolean). Sin Bean Validation, expone la entidad directamente (sin DTO).
- La lógica temporal de disponibilidad vive en `reservations`; el catálogo es CRUD plano de tarifas/flags.
- BD: mismo Supabase pooler 6543, mismas settings de Hikari; sin seeds ni migraciones.

### 4.4 `ms-andesstay-notify` (:8083) — Notificaciones, tickets y documentos (RabbitMQ)

- **Dependencias:** web, **spring-boot-starter-amqp**, validation, jackson. **Sin BD.** `@ConfigurationPropertiesScan` activo.
- **Endpoints** (`controller/NotifyController.java`, `/api/notify`, CORS `*`, controller sin lógica RabbitMQ — delega en publishers):
  | Método | Ruta | Entrada (`@Valid`) | Salida |
  |---|---|---|---|
  | POST | `/api/notify/send` | `SendNotificationRequest` (`reservationId`, `recipient` ≤200, `channel` ∈ EMAIL/SMS/PUSH `@Pattern`, `subject` ≤200, `body`) | texto con routingKey |
  | POST | `/api/notify/tickets` | `TicketEvent` (`reservationId`, `unitId`, `type`) — endpoint de prueba | texto con ticketId |
  | POST | `/api/notify/documents` | `DocumentEvent` (`reservationId`, `documentType`, `recipient`) — endpoint de prueba | texto con documentId |

- **Topología RabbitMQ declarada** (nombres **100% centralizados** en `andesstay.messaging.*` de `application.yml`, vía `MessagingProperties`; en `config/messaging/` están `NotificationsTopologyConfig`, `HousekeepingTopologyConfig`, `DocumentsTopologyConfig`, `DeadLetterConfig` y `TopologySupport`):

  | Elemento | Valor | Detalle |
  |---|---|---|
  | Exchange principal | `ex.andesstay.notifications` | Direct, durable |
  | Colas + bindings | `q.notify.email` ← `notify.email`; `q.notify.sms` ← `notify.sms`; `q.notify.push` ← `notify.push`; `q.housekeeping.tickets` ← `housekeeping.ticket`; `q.docs.generate` ← `docs.generate` | durables, todas con dead-lettering (`x-dead-letter-exchange=ex.andesstay.dlx`, `x-dead-letter-routing-key=dlq.routing.key`) |
  | DLX / DLQ | `ex.andesstay.dlx` / `q.andesstay.dlq` ← `dlq.routing.key` | Direct, durable |

  La topología se auto-declara al arrancar con el `RabbitAdmin` de `MessagingInfraConfig` (que también define el `Jackson2JsonMessageConverter` y el `RabbitTemplate`).
- **Consumidores** (`RabbitListenerConfig.java`): contenedor con **`AcknowledgeMode.MANUAL`**, `defaultRequeueRejected=false`, `missingQueuesFatal=false`, concurrencia 2, prefetch 10.
  - **Contrato ACK** (`messaging/AbstractDlqListener.java`, genérico `<T>`, probado con unit tests sin broker):
    1. Éxito → `channel.basicAck(tag,false)` + log `[ACK]`.
    2. `RecoverableException` (transitorio) → `RetryTemplate`: **3 intentos, 2 s de backoff fijo**; agotados → `basicNack(tag,false,false)` → DLQ (log `[DESCARTE->DLQ]`).
    3. `NonRecoverableException` (payload inválido) u otra excepción → `basicNack(requeue=false)` inmediato → DLQ. **Nunca se reencola** (no bucles).
  - Listeners (`@RabbitListener(queues="${andesstay.messaging.queues.*}")`): `EmailListener`, `SmsListener`, `PushListener`, `HousekeepingTicketListener`, `DocumentGenerationListener`, y `DeadLetterListener` (consume la DLQ: extrae de headers `x-death`/`x-exception-message` la cola origen, routing keys, motivo y contador, loguea en WARN y hace ACK — la DLQ no tiene DLX propia para evitar bucles).
- **Servicios de negocio:** `NotificationService` (valida coherencia payload/canal → `NonRecoverableException`; **simulación de fallo transitorio** si el destinatario está en `andesstay.notify.simulation.fail-recipients` — útil para demo reintento→DLQ), `HousekeepingService`, `DocumentService`.
- **Error handling:** `NotifyExceptionHandler` (`@RestControllerAdvice`): validación → **400** `{timestamp,status,error,fields{}}`; `IllegalArgumentException` → 400. Config exclusiva de mensajería separada de lógica de negocio por paquetes `messaging/notifications|housekeeping|documents|dlq`, `publishers` y `listeners`.
- Sin seguridad HTTP (endpoints abiertos); la credencial AMQP va en `application.yml` (env vars `RABBITMQ_HOST/PORT/USER/PASS/VHOST`).

### 4.5 `ms-andesstay-report` (:8084) — Reportes y KPIs

- **Dependencias:** web, data-jpa, postgresql, spring-kafka. **Sin seguridad.**
- **Kafka consumer** (`service/ReservationEventListener.java`): `@KafkaListener` topic **`reservations.events`**, group **`andesstay-report-group`**, `autoStartup="${kafka.enabled:false}"` (inactivo salvo `KAFKA_ENABLED=true`). Parseo tolerante del envelope, delega en `ReportService.recordReservationEvent`.
- **Modelo de datos:** tabla **`report_reservation_kpis`** (`ReservationKpi`): proyección **una fila por reserva** (upsert + limpieza de duplicados), campos `reservationId, unitId, status, createdAt, eventTimestamp, registeredAt`.
- **Reconciliación al arrancar** (`@EventListener(ApplicationReadyEvent)`): deduplica y **sincroniza contra `GET {reservations}/api/reservations`** vía `java.net.http.HttpClient` (1200/2000 ms). Endpoint `POST /api/report/sync` fuerza esta reconciliación.
- **KPIs** (`getKpis(range)`): desglose por status, total en rango (`last24h` default; `last7d`, `last30d`), reservas de hoy, total eventos, **tasa de ocupación activa** sobre inventario fijo de **20 unidades** (fallback referencial 78,5% si hay histórico sin activas) y `averageCycleHours` **hardcodeado en 3.5**. Top Units: nombre `"Unidad #id - AndesStay"` y **revenue estimado a $45.000 CLP/noche**.
- **Endpoints** (`/api/report`): `GET /kpis?range=`, `GET /top-units?range=`, `POST /events/simulate?reservationId&unitId&status&eventType&createdAt` (inyecta métrica sin Kafka), `POST /sync`. Salida KPIs con `dto/KpiResponseDTO` (`timeRange, totalEvents, statusBreakdown, activeOccupancyRate, reservationsToday, averageCycleHours, totalReservations`). CORS `*`.

### 4.6 `ms-andesstay-audit` (:8085) — Auditoría / timeline

- **Dependencias:** web, oauth2-resource-server (presente pero **no exigido**: `SecurityConfig` con `anyRequest().permitAll()`), data-jpa, postgresql, spring-kafka.
- **Kafka consumer** (`service/AuditService.java`): topics **`reservations.events`** y **`audit.timeline`**, group **`audit-group`**, `autoStartup="${kafka.enabled:false}"`. **Append-only**: siempre crea un nuevo `AuditLog`, nunca actualiza ni borra; parseo tolerante y hasta persiste mensaje irrecuperable como `EVENTO_KAFKA` crudo (nada se pierde).
- **Modelo:** tabla **`audit_logs`** (`eventType` NOT NULL, `actor` NOT NULL, `reservation_id`, `timestamp` NOT NULL, `details` ≤500, `payload` ≤2000). Getters alias para el frontend: `getAction()`/`getPerformedBy()`.
- **Repositorio:** ordenadas por timestamp DESC; `findByActor…`, `findByReservationId…`, `searchLogs(actor, eventType, reservationId)` con LIKE insensible y filtros nullables.
- **Endpoints** (`/api/audit`): `GET` con `?actor&eventType&reservationId`, `GET /actor/{actor}`, `GET /reservation/{reservationId}`, `POST` (registro directo con params + body opcional como payload). CORS `*`. Inmutabilidad por diseño de código (no hay constraints de BD).

### 4.7 `ms-andesstay-mqadmin` (:8086) — Administrador de RabbitMQ

- **Dependencias:** web, amqp, validation, actuator, oauth2-resource-server. **Sin BD** (registro en memoria).
- **Único microservicio de dominio con JWT obligatorio**: resource server Azure (mismos validadores TenantIssuer/Audience/roles que el BFF) + **`@PreAuthorize("hasRole('Admin')")` a nivel de clase en `MqAdminController`**. `/actuator/**` público, resto autenticado, 401 JSON manual.
- **Acceso al broker:** por **Spring AMQP** (AMQP 5672, no usa la Management HTTP API). `RabbitAdminConfig` declara `AmqpAdmin` con **`setAutoStartup(false)`** — nunca declara la topología de negocio al arrancar (el dueño es `notify`).
- **`RabbitAdminService`** (indicador 7: única puerta al `AmqpAdmin`): registro interno `ConcurrentHashMap` de recursos administrados (`managedQueues/Exchanges/Bindings`), **siembra** con `@PostConstruct seedDeclaredTopology()` la topología declarada en `andesstay.messaging` (sin declararla en el broker). Operaciones:
  - **Colas:** `createQueue` (args `x-message-ttl`, dead-lettering opcional ≥409 si ya gestionada), `deleteQueue`, `purgeQueue`, `listQueues` (enriquece con `getQueueInfo()` → `messageCount/consumerCount` en vivo).
  - **Exchanges:** `createExchange` (direct/topic/fanout/headers), `deleteExchange` (limpia bindings asociados), `listExchanges`.
  - **Bindings:** `createBinding` (valida que cola y exchange estén gestionados; clave única `exchange|destino|RK`), `deleteBinding`, `listBindings`.
  - **Publish de prueba:** `publishMessage` — destino exclusivo modo **cola** (exchange default `""`) o modo **exchange+RK**; JSON UTF-8; 409 si ambiguo, 404 si no gestionado.
  - Limitación documentada: Spring AMQP no lista recursos nativos del broker (haría falta la HTTP Management API) — de ahí el registro propio.
- **Endpoints** (`/api/mqadmin`, retornado por el BFF):
  | Método | Ruta | Códigos |
  |---|---|---|
  | GET/POST | `/queues` | 200 / 201 |
  | DELETE | `/queues/{name}` | 204 |
  | POST | `/queues/{name}/purge` | `{queue, purged}` |
  | POST | `/publish` | `{destination, routingKey, bytes, contentType}` |
  | GET/POST | `/exchanges` | 200 / 201 |
  | DELETE | `/exchanges/{name}` | 204 |
  | GET/POST | `/bindings` | 200 / 201 |
  | DELETE | `/bindings` | 204 (body con binding) |
- **Validación completa** (indicador 8): DTOs con `@NotBlank`, `@Size`, `@Pattern`, `@Min`, `@Valid`, `@Validated`. Patrón de nombres AMQP: `^(?!amq\.)[A-Za-z0-9._-]{3,255}$`; tipos de exchange restringidos. `MqAdminExceptionHandler`: 400 (`MethodArgumentNotValidException`/`ConstraintViolationException` con `fields{}`), 404 (`ResourceNotFoundException`), 409 (`IllegalArgumentException`), **502** (`AmqpException` — broker inalcanzable).

---

## 5. Mensajería — Resumen operativo

### RabbitMQ (comandos)
Exchange `ex.andesstay.notifications` (Direct) + DLX `ex.andesstay.dlx` + DLQ `q.andesstay.dlq`:

| Cola | Routing key | Productor | Consumidor | Significado |
|---|---|---|---|---|
| `q.notify.email` | `notify.email` | notify (`/send` EMAIL) | EmailListener | email de notificación |
| `q.notify.sms` | `notify.sms` | notify (`/send` SMS) | SmsListener | SMS |
| `q.notify.push` | `notify.push` | notify (`/send` PUSH) | PushListener | push |
| `q.housekeeping.tickets` | `housekeeping.ticket` | reservations (CHECKOUT) | HousekeepingTicketListener | ticket de limpieza |
| `q.docs.generate` | `docs.generate` | reservations (creada → voucher, confirmada → factura) | DocumentGenerationListener | voucher/factura |

Formato de mensajes: JSON (`Jackson2JsonMessageConverter` en productor y consumidor). ACK manual, reintentos 3×2 s solo para `RecoverableException`, DLQ siempre con `requeue=false`, y DLQ consumida por `DeadLetterListener` (log en WARN).

### Kafka (streaming, deshabilitado por defecto)
Topics: `reservations.events` (productor reservations; consumidores report y audit) y `audit.timeline`. Grupos: `andesstay-report-group`, `audit-group`. Sin `KAFKA_ENABLED=true`, reservations usa el **fallback HTTP** hacia report/audit, que funciona igual de end-to-end.

---

## 6. Infraestructura (`infra/`)

### 6.1 RabbitMQ (`infra/mq/compose.yml`)
- 2 nodos `rabbitmq:3.13-management` (`rabbitmq-1` :5672/:15672, `rabbitmq-2` :5673/:15673), vhost `andesstay`, user `andesstay`/`AndesStay2024!`, volúmenes persistentes, `rabbitmq.conf` (memory watermark 0.6, disk limit 1 GB). El cluster real no está formado (2 nodos independientes; se usa el principal).

### 6.2 Kafka (`infra/kafka/compose.yml`)
- 3 ZooKeeper (cp-7.6.1) + 3 Kafka brokers (:9092-9094, RF=3, 3 particiones, retención 168 h, connect timeout `${EC2_PUBLIC_IP:-localhost}`), `kafka-topics-init` crea `reservations.events` y `audit.timeline`, `kafka-ui` en :9000.

### 6.3 Aplicaciones (`infra/apps/compose.yml` + `.env`)
- 7 servicios con build context por microservicio, red `andesstay-apps`, `restart: unless-stopped`; bff con `depends_on` del resto. Env: `SERVICES_*_URL` (nombres de servicio en la red), DB y credenciales en `infra/apps/.env` (⚠️ versionado en claro: credenciales Supabase `DB_URL/DB_USER/DB_PASS`, `RABBITMQ_HOST=44.209.10.208` (EC2), `RABBITMQ_PASS`, `AZURE_ISSUER_URI/AZURE_AUDIENCE`; `KAFKA_ENABLED=false`).
- Los contenedores alcanzan los brokers por **host externo** (no están unidos a las redes `andesstay-mq`/`andesstay-kafka`), consistente con entorno de despliegue en EC2.

---

## 7. Configuración transversal / convenciones

- **Puertos:** 5173 (front) · 8080 bff · 8081 reservations · 8082 catalog · 8083 notify · 8084 report · 8085 audit · 8086 mqadmin · 9200/9000 UIs.
- **BD:** todos los servicios con BD comparten **Supabase PostgreSQL** por el pooler Transaction Mode :6543 (`prepareThreshold=0`), HikariCP `maximum-pool-size=3, minimum-idle=1, connection-timeout=20000`, `ddl-auto=update`. Sin migraciones (sin Flyway/Liquibase).
- **Properties/nombres:** la topología AMQP bajo `andesstay.messaging.*` en `application.yml` (notify y mqadmin comparten `MessagingProperties` idénticas); **sin literales de nombres AMQP en Java** (pauta EP3).
- **Idioma de logs:** mensajes en español con prefijos corchetes (`[ACK]`, `[REINTENTO]`, `[DESCARTE->DLQ]`, `[VALIDACION]`, `[JWT ISSUER MISMATCH]`).
- **Decisión transversal:** la publicación de eventos/correos **nunca bloquea el flujo principal** (async, fallback HTTP si Kafka caído, try/catch WARN si Rabbit caído).

---

## 8. Riesgos y observaciones detectados en el código (estado actual)

| # | Riesgo / observación | Dónde | Impacto |
|---|---|---|---|
| 1 | **Credenciales en claro versionadas**: Supabase (user/pass), RabbitMQ (`AndesStay2024!`), Azure IDs y endpoints EC2 en `application.properties` de reservations/catalog/report/audit, `Frontend/.env`, `infra/apps/.env`, `infra/mq/compose.yml` | backend + infra + front | Seguridad (riesgo R2 ya documentado) |
| 2 | Microservicios de dominio **sin filtro JWT** (catalog, notify, report, reservations abiertos; audit `permitAll` explícito) | backend | Solo mqadmin valida token; API interna expuesta |
| 3 | **Valores de negocio hardcodeados**: ocupación de respaldo 78.5%, `averageCycleHours=3.5`, revenue $45.000 CLP/noche, inventario fijo 20 unidades | `ReportService` | KPIs referenciales, no reales |
| 4 | **Cors `origins="*"`** a nivel controller en notify/report/audit/reservations vs CORS restrictivo del BFF | backend | Superficie CORS amplia |
| 5 | Envelope Kafka serializado con `String.format` **sin escape JSON** (fallaría si el detalle trae comillas) | `ReservationEventPublisher` | Fragilidad de payloads |
| 6 | BFF `WebClient` **sin timeouts** declarados y `@CrossOrigin("*")` por controller vs CorsConfigurationSource restrictivo | bff | Riesgo de colgar requests / CORS contradictorio |
| 7 | `POST /api/report/events/simulate` y `POST /api/audit` permiten escribir métricas/logs **sin autenticación** | report, audit | Posible inyección de datos |
| 8 | Notificaciones solo simuladas por log (sin SMTP/FCM/sms reales) | `NotificationService` | Alcance del EP3 |
| 9 | Catálogo expone entidades JPA sin DTO ni Bean Validation, sin delete ni GET por id expuestos | catalog | Calidad/robustez de API |
| 10 | Run de Kafka requiere `KAFKA_ENABLED=true`; en local por defecto se usa el fallback HTTP; en docker `.env` mantiene `KAFKA_ENABLED=false` | compose / properties | Operación |

---

## 9. Cómo correr todo (referencia rápida)

```bash
# 1) Infra RabbitMQ (y opcionalmente Kafka)
docker compose -f infra/mq/compose.yml up -d          # UI :15672 (andesstay / AndesStay2024!)
# docker compose -f infra/kafka/compose.yml up -d     # UI :9000 — solo si KAFKA_ENABLED=true

# 2) Microservicios (orden sugerido), en cada carpeta:
cd Backend-api/ms-andesstay-catalog      && .\mvnw.cmd spring-boot:run   # :8082
cd Backend-api/ms-andesstay-reservations && .\mvnw.cmd spring-boot:run   # :8081
cd Backend-api/ms-andesstay-notify       && .\mvnw.cmd spring-boot:run   # :8083 (dueño de topología)
cd Backend-api/ms-andesstay-report       && .\mvnw.cmd spring-boot:run   # :8084
cd Backend-api/ms-andesstay-audit        && .\mvnw.cmd spring-boot:run   # :8085
cd Backend-api/ms-andesstay-mqadmin      && .\mvnw.cmd spring-boot:run   # :8086 (JWT + Admin)
cd Backend-api/ms-andesstay-bff          && .\mvnw.cmd spring-boot:run   # :8080

# Kafka: arrancar con KAFKA_ENABLED=true (y KAFKA_BOOTSTRAP_SERVERS) en reservations/report/audit
# para usar streaming en vez del fallback HTTP.

# 3) Frontend
cd Frontend && npm install && npm run dev              # :5173 — apunta según .env (API Gateway AWS o BFF local)
```

**Flujos a prueba rápida:** crear reserva (POST `/api/reservations`) → documento VOUCHER en `q.docs.generate`; confirmar → FACTURA; pasar a CHECKOUT → ticket de limpieza; con `--andesstay.notify.simulation.fail-recipients=retry@andesstay.cl` se demuestra reintento 3×2 s → DLQ; listar KPIs `GET /api/report/kpis`, timeline `GET /api/audit`, topología y publish de prueba desde `/messaging` (rol Admin) o `POST /api/mqadmin/publish`.

---

## 10. Estado general (octubre 2026)

- **EP3 (RabbitMQ) completa**: las 15 tareas del plan están marcadas como realizadas en `tareas/README.md` — mensajería centralizada, ACK/DLQ, mqadmin con validación/seguridad, frontend `/messaging` y docs al día (`MENSAJERIA_RABBITMQ.md`).
- **Dual-broker coexistiendo**: Kafka para streaming (opcional/fallback HTTP) + RabbitMQ para comandos; documentada la separación de roles.
- **Cadena auth consistente**: MSAL (SPA, App Roles en `idTokenClaims.roles`) → BFF (valida issuer/audience y propaga Bearer) → mqadmin re-valida y exige `Admin`; resto de microservicios sin verificación interna.
