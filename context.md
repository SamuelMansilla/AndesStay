# AndesStay — Contexto del Proyecto y Plan de Integración EP3 (DSY1107)

> Documento de contexto para retomar el trabajo. Generado el **2026-10-15**.
> Fuente de requisitos: `EP3_DSY1107_Estudiante_encargo.pdf` (Evaluación Parcial N°3 — Desarrollo Cloud Native I).
> Objetivo de esta etapa: **incorporar RabbitMQ** al sistema ya existente (colas, exchanges y DLQs) para tareas asíncronas, **sin romper la lógica actual**.

---

## 1. Resumen del encargo (EP3)

- **Asignatura:** DSY1107 Desarrollo Cloud Native I · **Ponderación:** 12% · **Modalidad:** Parejas.
- **Foco:** agregar mensajería asíncrona con **RabbitMQ** (colas, exchanges, bindings y **DLQ**), manejando casos de uso como **notificaciones, generación de tickets o documentos**.
- **Entregable:** código fuente de frontend y backend vía GitHub (enlace a repositorio). El backend debe **compilar**, usar buenas prácticas y responder a pruebas básicas; el frontend debe estar completo, modular y sin errores de compilación.
- **Instrucciones específicas relevantes:**
  - Microservicios con **productores y consumidores** conectados a RabbitMQ.
  - Manejo de **mensajes no entregados → DLQ** y registro en **logs**.
  - **Microservicio administrador de RabbitMQ** que permita crear y eliminar colas, exchanges y bindings.
  - Integración backend ↔ BD cloud mediante entidades, repositorios y propiedades.
  - Backend con **filtros que validen el JWT** emitido por el IDaaS; frontend con login IDaaS y uso del JWT.
  - `.gitignore` correctos por tecnología.
- **Aspectos formales:** backend = varios microservicios **Java + Spring Boot**; el texto plantilla menciona **Angular**, pero **el docente autorizó usar React** (decisión confirmada — ver §10). El proyecto usa **React 19**.

### 1.1 Pauta de evaluación (indicadores y ponderación)

| # | Indicador | Peso | Estado actual |
|---|-----------|------|---------------|
| 1 | Nombres de colas/exchanges/bindings **centralizados** en `application.yml` o `@Configuration` | **12%** | Parcial (`notify`: hay claves en `.properties`, pero routing keys hardcodeadas en Java) |
| 2 | Beans de configuración `Queue`/`Exchange`/`Binding` **por cada caso de uso** | **13%** | Parcial (solo `notify`) |
| 3 | **Separación** config de mensajería vs lógica de negocio (clases dedicadas) | **10%** | No cumple (`NotifyController` mapea routing keys inline) |
| 4 | Consumidores con `@RabbitListener` **agrupados por dominio funcional** | **15%** | Parcial (un solo listener sin agrupación por dominio) |
| 5 | **ACK/NACK**, manejo explícito de errores, reintentos y ruteo a **DLQ** | **20%** | No cumple (sin ACK manual ni manejo de errores) |
| 6 | **Admin microservice** con endpoints REST (`POST /queues`, `DELETE /queues/{name}`, …) | **13%** | No existe |
| 7 | Lógica de administración encapsulada en un servicio (p. ej. `RabbitAdminService`) | **10%** | No existe |
| 8 | **Validación** de parámetros de entrada (DTO + anotaciones, no colas vacías) | **7%** | No existe para mensajería |

**Total: 100%.** Los ítems 5–8 (50%) están prácticamente en cero y son la prioridad máxima.

---

## 2. Contexto actual del proyecto

### 2.1 Estructura del repositorio

```
AndesStay/
├── README.md
├── context.md                     ← este documento
├── .gitignore
├── docs/
│   └── INTEGRACION_FRONTEND_BACKEND.md
├── Frontend/                      React 19 + TS + Vite (SPA)
│   ├── package.json
│   └── src/
│       ├── App.tsx, main.tsx, authConfig.ts, useApi.ts, ProtectedData.tsx
│       ├── api/        axiosClient, reservationsService, catalogService, reportService, auditService
│       ├── pages/      LoginPage, DashboardPage, ReservationsPage, CatalogPage, ReportsPage, AuditPage
│       ├── components/ Navbar, ProtectedRoute
│       ├── hooks/      useUserRole
│       └── types/      index.ts
├── Backend-api/                   6 microservicios Spring Boot (Maven)
│   ├── ms-andesstay-bff/          (8080) BFF + Spring Security OAuth2/OIDC (valida JWT Azure Entra ID)
│   ├── ms-andesstay-reservations/ (8081) CRUD reservas + ciclo de vida (Kafka producer + fallback HTTP)
│   ├── ms-andesstay-catalog/      (8082) Unidades, tarifas, disponibilidad (JPA)
│   ├── ms-andesstay-notify/       (8083) Notificaciones — ÚNICO con RabbitMQ hoy
│   ├── ms-andesstay-report/       (8084) KPIs (Kafka consumer)
│   └── ms-andesstay-audit/        (8085) Timeline inmutable (Kafka consumer)
└── infra/
    ├── mq/compose.yml             RabbitMQ 2 nodos + Management UI (5672/15672)
    ├── kafka/compose.yml          Zookeeper x3 + Kafka x3 + Kafka UI
    └── apps/compose.yml           Compose de los 6 microservicios
```

### 2.2 Stack y versiones

- **Backend:** Java 21, Spring Boot (⚠️ **versiones inconsistentes**: `notify` y demás heredan `3.3.4`; `reservations` declara `4.0.8`), Spring AMQP (`spring-boot-starter-amqp`), Spring Kafka, Spring Data JPA, PostgreSQL (Supabase), Lombok, Maven (`mvnw.cmd`).
- **Base de datos:** **Supabase PostgreSQL**, pooler *Transaction Mode* puerto **6543** (`prepareThreshold=0`), HikariCP `maximum-pool-size=3`.
- **Frontend:** React 19, TypeScript, Vite, React Router 7, Axios, MSAL (`@azure/msal-browser` / `@azure/msal-react`).
- **Mensajería hoy:** **Kafka** (streaming reservas → report/audit) + **RabbitMQ** (solo notificaciones en `notify`).
- **Auth:** Azure Entra ID (IDaaS). Solo el **BFF** valida el JWT (issuer v1/v2 y audience con/sin `api://`). Los microservicios de dominio **no** tienen filtro JWT (salvo `audit`, que tiene un `SecurityConfig` que permite todo).

### 2.3 Estado actual de RabbitMQ (lo único existente)

**Archivo:** `Backend-api/ms-andesstay-notify/src/main/java/com/duocuc/ms_andesstay_notify/config/RabbitMQConfig.java`

- Exchange: `ex.andesstay.notifications` (Direct, durable).
- Colas: `q.cmd.notify.email`, `q.cmd.notify.sms`, `q.cmd.notify.push` (durables, con DLX).
- DLX: `ex.andesstay.dlx` · DLQ: `q.cmd.notify.dlq`.
- **Problema:** los *routing keys* (`notify.email`, `notify.sms`, `notify.push`, `dlq.routing.key`) están **hardcodeados** en Java → incumple indicador 1.
- **Consumidor:** `NotificationListener` con `@RabbitListener` (un método por canal + uno para la DLQ). Sin ACK manual, sin `try/catch` explícito, sin ruteo controlado a DLQ.
- **Productor:** `NotifyController` (`POST /api/notify/send`) hace `switch` de canal → routing key **dentro del controlador** → incumple indicador 3.
- **Reintentos:** configurados por propiedades (`spring.rabbitmq.listener.simple.retry.*`, 3 intentos), pero sin estrategia de recuperación a DLQ.
- **`application.properties` Claves:** `andesstay.rabbitmq.exchange`, `.queue.email|sms|push|dlq`, `.exchange.dlx`. Puesta en marcha local: host `localhost:5672`, user `guest`.

### 2.4 Infra RabbitMQ disponible

`infra/mq/compose.yml`: 2 nodos (`rabbitmq-1`, `rabbitmq-2`) con `rabbitmq:3.13-management`, vhost `andesstay`, usuario `andesstay` / `AndesStay2024!`, AMQP 5672/5673 y UI 15672/15673. Ya está referenciado desde `infra/apps/compose.yml` (solo para `notify-svc`).

### 2.5 Patrones existentes a respetar

- Cada microservicio es un **proyecto Maven independiente** con su propio `Dockerfile` y `.properties`.
- El **BFF** expone `/api/**` y delega a cada servicio propagando el `Authorization: Bearer <jwt>`; `BffExceptionHandler` traduce fallos de conexión a **502**.
- Productores `reservations` publican eventos con envelope estándar (`eventId`, `traceId`, `correlationId`, `eventType`, `reservationId`, `unitId`, `status`, `details`).
- Manejo *fail-safe*: si el broker no está, el core **no se bloquea** (fallback HTTP / logs de advertencia).
- Frontend consume el BFF vía `axiosClient` (inyecta Bearer del token MSAL) y usa **URLs relativas** `/api/...`.

---

## 3. Brecha vs. la pauta (qué falta)

| Indicador | Falta |
|-----------|-------|
| 1 Centralización | Definir **toda** la topología (colas, exchanges, bindings, routing keys, DLQ) en `application.yml`/`@ConfigurationProperties` para **todos** los servicios. Eliminar strings en Java. |
| 2 Beans por caso de uso | Declarar `Queue`/`Exchange`/`Binding` para cada caso de uso nuevo (tickets, documentos, etc.). |
| 3 Separación | Mover el ruteo y el `convertAndSend` a un **servicio de mensajería** (`NotificationPublisher`, `TicketPublisher`, …). El controlador solo llama alto nivel. |
| 4 Consumidores por dominio | Reorganizar listeners en paquetes/`@Configuration` por dominio funcional (p. ej. `messaging/notifications`, `messaging/tickets`, `messaging/documents`). |
| 5 ACK/NACK + DLQ | `AcknowledgeMode.MANUAL`, `basicAck`/`basicNack(requeue=false)`, `RabbitListenerErrorHandler`, `MessageRecoverer` (reintentos → DLQ), diferenciar excepciones recuperables/no recuperables, loggear cada descarte. |
| 6 Admin REST | Nuevo **microservicio administrador** con endpoints REST coherentes. |
| 7 RabbitAdminService | Encapsular `AmqpAdmin` (`declareQueue/Exchange/Binding`, `deleteQueue/Exchange/Binding`) en un servicio dedicado. |
| 8 Validación | DTOs con `@NotBlank`, `@Pattern`, `@Size(…)`; rechazar nombres vacíos/`amq.*`/inválidos; `@Valid` + manejador global de errores con mensajes claros. |

---

## 4. Diseño propuesto

### 4.1 Microservicio administrador (nuevo)

**Opción A (CONFIRMADA): crear `ms-andesstay-mqadmin` dedicado (puerto 8086).**
- Alineado literalmente con "el microservicio administrador de RabbitMQ" y con la arquitectura de microservicios existente.
- Dependencias: `spring-boot-starter-web`, `spring-boot-starter-amqp`, `spring-boot-starter-validation`, `spring-boot-starter-test`.
- Se registra en el BFF (`BffMqAdminController` → `/api/mqadmin/**`) y en `infra/apps/compose.yml`.

> **Decisión tomada:** microservicio dedicado nuevo. (Se descarta ampliar `ms-andesstay-notify`.)

### 4.2 Topología RabbitMQ (casos de uso)

Exchange principal **Topic** `ex.andesstay.topic` con routing keys por dominio; DLX directo `ex.andesstay.dlx`.

| Caso de uso | Cola | Routing key | Grupo/dominio | Productor | Consumidor |
|-------------|------|-------------|---------------|-----------|------------|
| Notificación email | `q.notify.email` | `notify.email` | notifications | reservations / notify / admin | notify |
| Notificación sms | `q.notify.sms` | `notify.sms` | notifications | notify | notify |
| Notificación push | `q.notify.push` | `notify.push` | notifications | notify | notify |
| Ticket de housekeeping | `q.housekeeping.tickets` | `housekeeping.ticket` | housekeeping | reservations (al CHECKOUT) | notify (o nuevo ms) |
| Generación de documento (voucher/factura) | `q.docs.generate` | `docs.generate` | documents | reservations (al crear/confirmar) | notify/report |
| DLQ compartida | `q.andesstay.dlq` | `dlq.routing.key` | — | (auto) | todos (log + alerta) |

- **Decisión tomada:** **migrar** a la convención consistente (`q.<dominio>.<acción>`, `ex.andesstay.<dominio>`). Los nombres actuales (`ex.andesstay.notifications`, `q.cmd.notify.*`) se reemplazan de forma controlada.
- Cada cola con `x-dead-letter-exchange=ex.andesstay.dlx` y `x-dead-letter-routing-key=dlq.routing.key` (parametrizado, no hardcodeado).

**Casos de uso confirmados:** notificaciones (email/sms/push) + **tickets de housekeeping** + **generación de documentos (vouchers/facturas)**.

### 4.3 Centralización de nombres (indicador 1 y 2)

- Todas las claves en **`application.yml`** bajo un árbol `andesstay.messaging.*`, y una clase `@ConfigurationProperties(prefix="andesstay.messaging")` (`MessagingProperties`) que expone exchange, dlx, routing keys y mapa de colas.
- **Ningún** nombre literal en clases de negocio; los `@RabbitListener(queues = "${...}")` usan claves de propiedad.
- Para el admin, un endpoint/consola de "topología declarada" que lee de la config.

### 4.4 Separación de responsabilidades (indicador 3)

- `config/` → solo beans de infraestructura (Exchange, Queue, Binding, Converter, `RabbitAdmin`, container factory).
- `messaging/notifications|housekeeping|documents` → **publishers** (productores) y **listeners** (consumidores) por dominio.
- Controladores → delegación a publishers/servicios.
- `service/` → lógica de negocio pura.

### 4.5 Consumidores con ACK/NACK + DLQ (indicador 5, el de mayor peso)

- `SimpleRabbitListenerContainerFactory` con **`AcknowledgeMode.MANUAL`**.
- Listener recibe `(NotificationEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag, @Header(AmqpHeaders.REDELIVERED) boolean redelivered)`.
  - Éxito → `channel.basicAck(tag, false)`.
  - Error **recuperable** → reintento acotado (`RetryInterceptor` con `maxAttempts`); agotado → `basicNack(tag,false,false)` (a DLQ).
  - Error **no recuperable** (payload inválido) → `AmqpRejectAndDontRequeueException` → DLQ directo.
- Configurar `SimpleMessageListenerContainer.setDefaultRequeueRejected(false)` y `setMissingQueuesFatal(false)`.
- `RepublishMessageRecoverer` o `RejectAndDontRequeueRecoverer` para enviar a DLX con headers (`x-exception-message`, `x-original-queue`, `x-death`).
- **Loggear** en WARN/ERROR todo mensaje descartado, con `deliveryTag` y reserva/ID.
- Listener de DLQ que registra el fallo (y opcionalmente alerta).

### 4.6 Admin REST + RabbitAdminService (indicadores 6 y 7)

`RabbitAdminService` (wrapper de `org.springframework.amqp.core.AmqpAdmin`): `createQueue`, `deleteQueue`, `purgeQueue`, `createExchange`, `deleteExchange`, `createBinding`, `deleteBinding`, `listQueues`, `listExchanges`, `listBindings`.

Endpoints REST (controlador **delgado**, sin detalles de RabbitMQ):

| Método | Ruta | Acción |
|--------|------|--------|
| `GET` | `/api/mqadmin/queues` | Listar colas + profundidad |
| `POST` | `/api/mqadmin/queues` | Crear cola |
| `DELETE` | `/api/mqadmin/queues/{name}` | Eliminar cola |
| `POST` | `/api/mqadmin/queues/{name}/purge` | Purgar |
| `GET` | `/api/mqadmin/exchanges` | Listar exchanges |
| `POST` | `/api/mqadmin/exchanges` | Crear exchange |
| `DELETE` | `/api/mqadmin/exchanges/{name}` | Eliminar exchange |
| `GET` | `/api/mqadmin/bindings` | Listar bindings |
| `POST` | `/api/mqadmin/bindings` | Crear binding |
| `DELETE` | `/api/mqadmin/bindings` | Eliminar binding |

### 4.7 Validación (indicador 8)

DTOs con Jakarta Validation:
- `QueueRequestDTO`: `@NotBlank @Pattern(regexp="^[A-Za-z0-9._-]{3,255}$") name`, `@NotNull Boolean durable`, `@Min/@Max` para `ttl`, flags `autoDelete`, `deadLetter` (opcional).
- `ExchangeRequestDTO`: `name`, `type` ∈ {direct, topic, fanout, headers}, `durable`, `autoDelete`.
- `BindingRequestDTO`: `@NotBlank` queue, `@NotBlank` exchange, `routingKey`.
- Regla extra: rechazar nombres que empiecen por `amq.` y colisiones de tipo de exchange existente.
- `@Valid` en controladores + `@RestControllerAdvice` (`MqAdminExceptionHandler`) → **400** con mensaje claro (`{"error": "...", "fields": {...}}`).

### 4.8 Seguridad

- El admin ms se expone a través del **BFF** (`/api/mqadmin/**`), que ya valida el JWT (indicador de "filtros JWT").
- Recomendado: añadir `spring-boot-starter-oauth2-resource-server` + `SecurityConfig` (reutilizando `AudienceValidator`/`TenantIssuerValidator`) al admin ms, y `@PreAuthorize("hasRole('Admin')")` en los controladores. Alternativa mínima: permitir solo desde la red interna/compose.

---

## 5. Plan de implementación por fases

> Cada fase indica el/los indicador(es) de la pauta que cubre y archivos afectados. Estimación total ~2 semanas (pareja).

### Fase 0 — Preparación (0.5 día)
- [ ] Unificar versiones de Spring Boot en la familia de microservicios (según decisión §9/⚠️R3).
- [ ] Levantar RabbitMQ local: `docker compose -f infra/mq/compose.yml up -d` (UI: http://localhost:15672).
- [ ] Rama `feature/ep3-rabbitmq`.

### Fase 1 — Centralización de topología y beans (ind. 1 y 2 · 25%)
- [ ] Crear `MessagingProperties` (`@ConfigurationProperties`) y migrar claves a `application.yml`.
- [ ] Mover routing keys y nombres de DLQ a propiedades (quitar literales en `RabbitMQConfig` y `NotifyController`).
- [ ] Declarar beans `Queue`/`Exchange`/`Binding` para: notificaciones (3 canales), tickets, documentos y DLQ.
- [ ] Documentar cada ruta de mensajería en el propio `@Configuration`.

### Fase 2 — Separación y publishers (ind. 3 · 10%)
- [ ] `NotificationPublisher` (email/sms/push) que encapsula `convertAndSend`.
- [ ] `TicketPublisher` y `DocumentPublisher` (o un `MessagingPublisher` genérico por dominio).
- [ ] Refactor `NotifyController`: solo valida DTO y llama al publisher.
- [ ] (Reservations) publicar `housekeeping.ticket` en CHECKOUT y `docs.generate` al confirmar, vía servicio de mensajería propio (`ReservationMessagingPublisher`), sin bloquear el core.

### Fase 3 — Consumidores por dominio + ACK/DLQ (ind. 4 y 5 · 35%, prioridad)
- [ ] Reorganizar listeners en `messaging/<dominio>`.
- [ ] `SimpleRabbitListenerContainerFactory` con `AcknowledgeMode.MANUAL`.
- [ ] ACK/NACK manual, `RabbitListenerErrorHandler`, `RetryInterceptor` + `MessageRecoverer`.
- [ ] Diferenciar errores recuperables vs no recuperables; `AmqpRejectAndDontRequeueException`.
- [ ] Listener de DLQ con logging estructurado (y contador/alerta).

### Fase 4 — Microservicio administrador (ind. 6, 7 y 8 · 30%)
- [ ] Crear `Backend-api/ms-andesstay-mqadmin` (Maven, Java 21, puerto 8086) + `Dockerfile`.
- [ ] `RabbitAdminService` encapsulando `AmqpAdmin`.
- [ ] `MqAdminController` con endpoints de §4.6.
- [ ] DTOs con validación + `@RestControllerAdvice`.
- [ ] `SecurityConfig` (recurso OAuth2) + `@PreAuthorize("hasRole('Admin')")`.
- [ ] `BffMqAdminController` en el BFF (`/api/mqadmin/**`) que propaga el Bearer.
- [ ] Registrar en `infra/apps/compose.yml` y variables de entorno.

### Fase 5 — Frontend (opcional pero valorado)
- [ ] Página `MessagesAdminPage` (ruta `/messaging`, rol `Admin`) que liste/crea/elimine colas, exchanges y bindings vía `/api/mqadmin/**`.
- [ ] `mqAdminService.ts` con `axiosClient`.
- [ ] Enlace en `Navbar`.

### Fase 6 — Pruebas, documentación y entrega
- [ ] Pruebas: test de contexto por ms, test unitario de `RabbitAdminService` (mock `AmqpAdmin`), test de validación de DTOs (400), test de listener (ACK/DLQ con `@SpringBootTest` + broker de test o `spring-rabbit-test`).
- [ ] Actualizar `README.md` y `docs/` (topología, endpoints admin, cómo probar DLQ).
- [ ] Verificar `.gitignore` (no subir `target/`, `node_modules/`, secretos).
- [ ] `mvnw.cmd -q clean package` en cada ms + `npm run build` en Frontend.
- [ ] Tag/PR y enlaces de GitHub para AVA.

---

## 6. Estructura de archivos objetivo (admin + notify)

```
Backend-api/ms-andesstay-mqadmin/
├── pom.xml
├── Dockerfile
└── src/main/
    ├── java/com/duocuc/ms_andesstay_mqadmin/
    │   ├── MsAndesstayMqadminApplication.java
    │   ├── config/           RabbitMQConfig, SecurityConfig, RabbitAdminConfig
    │   ├── controller/       MqAdminController
    │   ├── service/          RabbitAdminService
    │   ├── dto/              QueueRequestDTO, ExchangeRequestDTO, BindingRequestDTO
    │   ├── exception/        MqAdminExceptionHandler
    │   └── model/            QueueInfo, ExchangeInfo, BindingInfo
    └── resources/
        └── application.yml   andesstay.messaging.* + rabbitmq + port 8086

Backend-api/ms-andesstay-notify/src/main/java/.../
    ├── config/               RabbitMQConfig (beans), ListenerContainerConfig, MessagingProperties
    ├── messaging/
    │   ├── notifications/    NotificationPublisher, EmailListener, SmsListener, PushListener
    │   ├── housekeeping/     HousekeepingTicketListener
    │   ├── documents/        DocumentGenerationListener
    │   └── dlq/              DeadLetterListener
    ├── controller/           NotifyController (delegado al publisher)
    ├── model/                NotificationEvent, TicketEvent, DocumentEvent
    └── service/              lógica de negocio
```

---

## 7. Convenciones de nombres

- Exchange: `ex.andesstay.<dominio>` (p. ej. `ex.andesstay.notifications`).
- Cola: `q.<dominio>.<acción>` (p. ej. `q.notify.email`, `q.housekeeping.tickets`, `q.docs.generate`).
- Routing key: `<dominio>.<acción>` (p. ej. `notify.email`, `housekeeping.ticket`).
- DLX: `ex.andesstay.dlx` · DLQ: `q.andesstay.dlq` · routing key DLQ: `dlq.routing.key`.
- Caracteres permitidos: `[A-Za-z0-9._-]`, longitud 3–255, prohibido prefijo `amq.`.

---

## 8. Criterios de aceptación (para lograr 100% en la pauta)

- [ ] **Ningún** nombre de cola/exchange/binding/routing key aparece como literal en clases Java (todo vía propiedades).
- [ ] Existe una clase de configuración con beans `Queue`/`Exchange`/`Binding` **por cada caso de uso**, claramente identificados y comentados.
- [ ] Los controladores no contienen lógica de RabbitMQ; delegan a servicios/publishers.
- [ ] Todos los consumidores usan `@RabbitListener` y están **agrupados por dominio funcional**.
- [ ] Cada consumidor hace **ACK** explícito y, ante error, decide reintento / NACK / DLQ; todo descarte queda en **logs** con contexto.
- [ ] El admin ms expone REST coherente (`POST /queues`, `DELETE /queues/{name}`, exchanges y bindings).
- [ ] Toda la lógica de `AmqpAdmin` está en `RabbitAdminService`; los controladores llaman métodos de alto nivel.
- [ ] Los endpoints validan entrada (DTO + `@Valid`); no se pueden crear colas con nombre vacío/inválido; errores claros (400).
- [ ] Todos los microservicios compilan (`mvnw.cmd clean package`) y el frontend buildea (`npm run build`).

---

## 9. Riesgos y observaciones

| ID | Riesgo / observación | Impacto | Recomendación |
|----|----------------------|---------|---------------|
| R1 | El texto del encargo menciona frontend **Angular**, pero el proyecto es **React 19**. | Formal | ✅ **Resuelto:** el docente autorizó mantener React. Documentar la autorización. |
| R2 | **Secretos** de BD (password Supabase) y de RabbitMQ están en `.properties`/compose versionados. | Seguridad | Externalizar a variables de entorno y documentar; rotar credenciales si aplica. |
| R3 | **Versiones Spring Boot inconsistentes** (`3.3.4` vs `4.0.8`). | Compilación/arranque | Unificar (p. ej. 3.3.x) y verificar compatibilidad de `spring-boot-starter-amqp`. |
| R4 | Coexistencia Kafka (streaming) + RabbitMQ (tareas). | Complejidad | Mantener Kafka para streaming y RabbitMQ para comandos/colas; documentar el límite. |
| R5 | Solo el **BFF** valida JWT; microservicios de dominio abiertos. | Seguridad | Al menos proteger el admin ms (recurso OAuth2 o acceso solo por red interna/BFF). |
| R6 | `notify` no tiene filtro de seguridad. | Seguridad | Idem R5. |
| R7 | AVCK manual con broker caído puede colgar consumidores. | Operación | `missingQueuesFatal=false`, timeouts y reconexión; probar con broker apagado. |

---

## 10. Decisiones tomadas y preguntas abiertas

**Decisiones confirmadas (2026-10-15):**
1. **Admin ms:** se crea microservicio dedicado **`ms-andesstay-mqadmin`** (puerto 8086). ✅
2. **Frontend:** se mantiene **React 19**; el docente autorizó usarlo en vez de Angular. ✅
3. **Nombres de topología:** se **migra** a la convención `q.<dominio>.<acción>` / `ex.andesstay.<dominio>`. ✅
4. **Casos de uso:** notificaciones + **tickets de housekeeping** + **documentos (vouchers/facturas)**. ✅

**Pendientes de confirmar:**
1. **Spring Boot:** ¿unificamos versiones de los microservicios y cuál usamos (p. ej. 3.3.x)?
2. **Seguridad admin ms:** ¿recurso OAuth2 con rol `Admin` (recomendado) o solo acceso por red interna/BFF?
3. **Secretos (R2):** ¿rotar y externalizar credenciales de Supabase/RabbitMQ ahora o al cierre?

---

## 11. Cómo levantar y probar (referencia rápida)

```bash
# 1. Infra RabbitMQ (local)
docker compose -f infra/mq/compose.yml up -d
# UI: http://localhost:15672  (andesstay / AndesStay2024!)

# 2. Microservicios (cada uno en su carpeta)
cd Backend-api/ms-andesstay-notify      && .\mvnw.cmd spring-boot:run
cd Backend-api/ms-andesstay-mqadmin     && .\mvnw.cmd spring-boot:run   # futuro
cd Backend-api/ms-andesstay-bff         && .\mvnw.cmd spring-boot:run

# 3. Frontend
cd Frontend && npm install && npm run dev   # http://localhost:5173

# 4. Prueba manual DLQ
# POST /api/notify/send  con payload inválido → observar reintentos y llegada a q.andesstay.dlq + logs.
```

---

### Cómo retomar este documento

- **Estado:** diseño cerrado; decisiones §10 confirmadas. **Fase 0 por iniciar** (aún sin código de la EP3).
- **Siguiente acción sugerida:** ejecutar las tareas en orden desde **[`tareas/README.md`](tareas/README.md)** (una tarea por commit, con rollback independiente); empezar por `tareas/fase-00-preparacion.md` y `fase-01`, y luego la Fase 1 de mensajería (tareas 02–03).
- Al reabrir el proyecto, leer en este orden: `context.md` → `tareas/README.md` → `README.md` → `docs/INTEGRACION_FRONTEND_BACKEND.md`.
