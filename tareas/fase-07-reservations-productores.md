# Tarea 07 — reservations: publicar tickets y documentos

- **Fase / Nº:** 07
- **Indicador(es) de la pauta:** **Indicador 1 (12%)**, **Indicador 3 (10%)** — productor con nombres centralizados y separación
- **Depende de:** 03
- **Rama:** `feature/ep3-rabbitmq`
- **Commit sugerido:** `feat(reservations): publicar tickets de housekeeping y generación de documentos vía RabbitMQ`
- **Riesgo de rollback:** bajo/medio (añade publicación; no debe bloquear el core)

## Objetivo
Convertir `ms-andesstay-reservations` en **productor** de eventos de negocio hacia RabbitMQ: al **CHECKOUT** generar un ticket de housekeeping y al **confirmar/crear** generar un documento (voucher/factura), sin bloquear la respuesta HTTP ni romper el flujo existente (Kafka/fallback HTTP).

## Alcance
**Incluye**
- `spring-boot-starter-amqp` en el `pom.xml`.
- Config AMQP + `MessagingProperties` local (mismos nombres de topología).
- `ReservationMessagingPublisher` (o extender `ReservationEventPublisher`) que publica de forma asíncrona y tolerante a fallos.
- Disparo desde `ReservationService` según transición de estado.

**NO incluye**
- Consumidores en `reservations` (los consumidores viven en `notify`).
- ACK/DLQ (es un productor; lo maneja el consumidor).

## Prerrequisitos
- Tarea 03 completada (topología definida).
- **Decisión:** confirmar que el consumidor final de `q.housekeeping.tickets` y `q.docs.generate` es `notify` (u otro ms).

## Pasos
1. Agregar dependencia AMQP al `pom.xml` de `reservations`.
2. Añadir a `application.properties`/`.yml`:
   - `spring.rabbitmq.*` (host/puerto/user/pass por variables de entorno).
   - `andesstay.messaging.*` (misma convención que `notify`).
3. Crear `config/MessagingProperties.java` + `config/RabbitMQConfig.java` (RabbitTemplate con converter JSON).
   > Opcionalmente, extraer estos nombres a un **módulo común** si se desea evitar duplicación (no obligatorio).
4. Crear `messaging/ReservationMessagingPublisher.java`:
   - `publishTicketEvent(...)` → `housekeeping.ticket`.
   - `publishDocumentEvent(...)` → `docs.generate`.
   - `try/catch` con log WARN; **nunca** propaga excepción al flujo de negocio.
5. En `ReservationService`:
   - En transición a `CHECKOUT` → `publishTicketEvent(...)`.
   - En `CREADA`/`CONFIRMADA` → `publishDocumentEvent(...)`.
   - Mantener intactos Kafka y el fallback HTTP.
6. Verificar idempotencia/duplicados y que no se bloquea el endpoint.

## Archivos a crear / modificar
| Acción | Archivo |
|--------|---------|
| Modificar | `.../ms-andesstay-reservations/pom.xml` |
| Modificar | `.../ms-andesstay-reservations/src/main/resources/application.properties` |
| Crear | `.../reservations/config/MessagingProperties.java`, `RabbitMQConfig.java` |
| Crear | `.../reservations/messaging/ReservationMessagingPublisher.java` |
| Modificar | `.../reservations/service/ReservationService.java` |

## Criterios de aceptación (Definition of Done)
- [x] Al hacer CHECKOUT se publica un mensaje en `q.housekeeping.tickets`.
- [x] Al crear/confirmar una reserva se publica un mensaje en `q.docs.generate`.
- [x] Si RabbitMQ no está disponible, el endpoint **responde igual** (no bloquea).
- [x] No hay literales de topología en la lógica de negocio.
- [x] Compila y arranca.

## Cómo verificar
```bash
cd Backend-api/ms-andesstay-reservations && .\mvnw.cmd spring-boot:run
# Crear reserva
curl -X POST http://localhost:8081/api/reservations -H "Content-Type: application/json" \
  -d '{"guestEmail":"a@b.cl","unitId":1,"checkInDate":"2026-11-01","checkOutDate":"2026-11-03"}'
# Confirmar y luego CHECKOUT; revisar en la UI de RabbitMQ el incremento en las colas
```
- Con RabbitMQ **apagado**, repetir: el endpoint debe responder sin error.

## Rollback
```bash
git revert <hash>
```
> Revertir deja `reservations` igual que antes (solo Kafka/fallback). Seguro.

## Notas
- Regla clave del proyecto: **no bloquear el core**. La publicación va en `try/catch` y preferentemente con callback asíncrono del `RabbitTemplate`.
- Documentar la decisión de duplicar `MessagingProperties` por microservicio (vs módulo compartido).

---

## Resultado — 2026-10-15

### Disparadores implementados

| Transición de la reserva | Método del publisher | Routing key | Cola destino | Consumidor |
|--------------------------|----------------------|-------------|--------------|------------|
| Creación (`CREADA`) | `publishVoucherEvent(id, email)` | `docs.generate` | `q.docs.generate` | `DocumentGenerationListener` (notify) |
| Paso a `CONFIRMADA` | `publishInvoiceEvent(id, email)` | `docs.generate` | `q.docs.generate` | `DocumentGenerationListener` (notify) |
| Paso a `CHECKOUT` | `publishTicketEvent(id, unitId)` | `housekeeping.ticket` | `q.housekeeping.tickets` | `HousekeepingTicketListener` (notify) |

Se mantuvieron **intactos** Kafka (`publishReservationEvent`) y el fallback HTTP a report/audit: la mensajería AMQP se suma como tercer canal, no reemplaza nada.

### Archivos

| Acción | Archivo | Rol |
|--------|---------|-----|
| Modificar | `ms-andesstay-reservations/pom.xml` | `spring-boot-starter-amqp` |
| Modificar | `.../resources/application.properties` | `spring.rabbitmq.*` (+ `connection-timeout=2000`), `andesstay.messaging.*` |
| Crear | `.../config/MessagingProperties.java` | topología centralizada (indicador 1) |
| Crear | `.../config/RabbitMQConfig.java` | `Jackson2JsonMessageConverter` + `RabbitTemplate` |
| Crear | `.../messaging/ReservationMessagingPublisher.java` | productor por caso de uso |
| Crear | `.../messaging/event/TicketEvent.java`, `DocumentEvent.java` | eventos de negocio |
| Modificar | `.../service/ReservationService.java` | disparo según transición de estado |
| Modificar | `MsAndesstayReservationsApplication.java` | `@ConfigurationPropertiesScan` |

### Decisiones de diseño

**1. Publicación asíncrona + timeout corto de conexión.** `publish(...)` ejecuta `convertAndSend` en un `CompletableFuture` (mismo patrón que el fallback HTTP ya existente en `ReservationEventPublisher`) y se bajó `spring.rabbitmq.connection-timeout` de 60 s a **2 s**. Sin esto, un `convertAndSend` síncrono con el broker caído retendría el hilo de la petición hasta 60 s y el DoD "no bloquea" no se cumpliría. La excepción nunca se propaga: sólo queda en `WARN`.

**2. `notify` es el único dueño de la topología.** `RabbitMQConfig` de reservations declara sólo el converter y el template; el exchange, las colas y los bindings los declara `notify` (tarea 03). Evita dos servicios compitiendo por la misma definición.

**3. `MessagingProperties` se duplica por microservicio** en vez de extraerse a un módulo común. Los microservicios no comparten artefacto (se despliegan y versionan por separado) y lo que se duplica son nombres de configuración, no lógica; el contrato real está fijado por la topología de la tarea 03. Queda documentado en el Javadoc de la clase.

**4. Los eventos se modelan por servicio.** `cl.duoc…messaging.event.TicketEvent`/`DocumentEvent` replican los nombres de campo que espera `notify`. Verificado en la integración real: el `__TypeId__` que agrega el productor apunta a una clase que el consumidor no conoce, pero el converter resuelve por el tipo inferido del método (`TypePrecedence.INFERRED`, el valor por defecto), así que el binding JSON funciona sin ningún mapeo extra.

### Verificación (flujo real, extremo a extremo)

**1. Compilación:** `.\\mvnw.cmd -B clean package -DskipTests` → `BUILD SUCCESS`, 0 `[ERROR]`, 0 `[WARNING]`.

**2. Arranque:** `Started MsAndesstayNotifyApplication in 10.522 seconds` y `Started MsAndesstayReservationsApplication in 26.532 seconds`.

**3. Flujo completo** (reserva #13, unidad 77, huésped `final@andesstay.cl`):

| Petición | Respuesta | Tiempo |
|----------|-----------|--------|
| `POST /api/reservations` | `201` + reserva `CREADA` | 1.99 s |
| `PUT /api/reservations/13/status` (`CONFIRMADA`) | `200` | 0.60 s |
| `PUT /api/reservations/13/status` (`CHECKOUT`) | `200` | 0.39 s |

Productor:
```text
INFO [RABBITMQ] Publicado VOUCHER de la reserva #13 en el exchange ex.andesstay.notifications con routing key docs.generate
INFO [RABBITMQ] Publicado FACTURA de la reserva #13 en el exchange ex.andesstay.notifications con routing key docs.generate
INFO [RABBITMQ] Publicado ticket de housekeeping de la reserva #13 en el exchange ex.andesstay.notifications con routing key housekeeping.ticket
```
Todas las publicaciones salen en hilos `onPool-worker-*` (nunca en el hilo `nio-8081-exec-*` que atiende la petición).

Consumidor (notify):
```text
INFO [DOCS] Documento aa3d6951… (VOUCHER) generado para la reserva #13 y enviado a final@andesstay.cl
INFO [DOCS] Documento 4756ea8d… (FACTURA) generado para la reserva #13 y enviado a final@andesstay.cl
INFO [HOUSEKEEPING] Ticket 8edc0803… (LIMPIEZA) registrado para la reserva #13 de la unidad #77
INFO [ACK] canal=DOCUMENTS deliveryTag=1 reentregado=false evento=DocumentEvent{documentId='4756ea8d…', reservationId=13, documentType='FACTURA', recipient='final@andesstay.cl'}
INFO [ACK] canal=DOCUMENTS deliveryTag=1 reentregado=false evento=DocumentEvent{documentId='aa3d6951…', reservationId=13, documentType='VOUCHER', recipient='final@andesstay.cl'}
INFO [ACK] canal=HOUSEKEEPING deliveryTag=1 reentregado=false evento=TicketEvent{ticketId='8edc0803…', reservationId=13, unitId=77, type='LIMPIEZA', …}
```

Los documentos se procesan en los dos consumidores del dominio (`ntContainer#1-1` y `#1-2`), por eso el orden de los ACK puede variar entre corridas; lo relevante es que cada evento se confirma una sola vez con `deliveryTag=1` y `reentregado=false`.

**4. Broker:** las 6 colas en 0 mensajes y 0 sin confirmar (todo consumido y confirmado). La DLQ en 0.

**5. RabbitMQ no disponible** (instancia en el puerto `5679`, puerto cerrado):

| Escenario | Respuesta | Tiempo |
|-----------|-----------|--------|
| Broker disponible (5672) | `201` | 2.06 s |
| Broker inalcanzable (5679) | `201` | 1.91 s |

El tiempo no depende del broker (lo domina la escritura en la base). Log del productor:
```text
WARN [RABBITMQ] No se pudo publicar VOUCHER de la reserva #12 (no bloquea el core): java.net.ConnectException: Connection refused: getsockopt
```
El endpoint respondió `201` y el fallo quedó registrado sin afectar el core.

**6. Sin literales de topología:** grep de `"q.|"ex.|"notify.|housekeeping.ticket|docs.generate|dlq.routing.key|andesstay.rabbitmq` sobre `src/main/java` de reservations excluyendo comentarios → **0 coincidencias**; sobre `ReservationService` (la lógica de negocio) → **0**.

### Hallazgo sobre el cluster local

`localhost:5672` (rabbitmq-1) y `localhost:5673` (rabbitmq-2) están ambos escuchando: el segundo nodo del `compose` **no comparte la topología** (los nodos no están realmente en clúster, ver `context.md`). Al publicar contra 5673 el mensaje se pierde de forma silenciosa, porque el exchange no existe en ese nodo y AMQP no devuelve error al productor sin *publisher returns*. Es una limitación del entorno local, no del código: en la verificación se usó 5672 (el nodo con la topología del vhost `andesstay`). Queda como recomendación para la entrega: usar un único nodo o habilitar `mandatory` + *publisher confirms* si se quisiera detectar ese caso.

### Datos de prueba

La verificación creó reservas reales en la base Supabase compartida (`id` 9 a 13, huéspedes `huesped@`, `sinbroker@`, `bench@` y `final@andesstay.cl`).

### Rollback
```bash
git revert <hash>
```
Deja `reservations` igual que antes (sólo Kafka + fallback HTTP). No afecta la topología del broker.

