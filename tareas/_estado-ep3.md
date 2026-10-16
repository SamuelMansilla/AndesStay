# Estado de la sesión — EP3 RabbitMQ

- **Rama:** `feature/ep3-rabbitmq` (tareas 00–14; tip de la sesión 2: `ed89e23` + commit de la tarea 14)
- **Fecha:** 2026-10-15 (sesión 1: tareas 00–07) · 2026-10-16 (sesión 2: tareas 08–14)
- **Push:** la rama se pusheó tras la sesión 1; los commits de las tareas 08–14 están **sin pushear**
- **Árbol de trabajo:** limpio al cierre de cada sesión

Este documento es el **traspaso de la sesión**: qué quedó hecho, qué decisiones se tomaron y por qué, qué falta y cuál era el próximo paso acordado. El detalle fino de cada tarea está en su propio archivo (`fase-0X-*.md`, sección *Resultado*).

---

## 1. Tareas completadas

| # | Tarea | Commit | Estado |
|---|-------|--------|--------|
| 00 | Preparación (rama, `rabbitmq.conf`, línea base) | `d1a2679`, `08a6103` | [x] |
| 01 | Unificar Spring Boot 3.3.4 + Java 21 en los 6 microservicios | `487ce04` | [x] |
| 02 | notify: centralizar topología en `MessagingProperties` | `fdf7921` | [x] |
| 03 | notify: beans `Queue`/`Exchange`/`Binding` por caso de uso | `c3234c8` | [x] |
| 04 | notify: publishers por dominio y limpieza de `NotifyController` | `d8d19dd` | [x] |
| 05 | notify: ACK manual, reintentos acotados y DLQ | `6f59822` | [x] |
| 06 | notify: consumidores agrupados por dominio | `c79d5e1` | [x] |
| 07 | reservations: publicar tickets y documentos | `cfa6433` | [x] |
| 08 | mqadmin: esqueleto del microservicio | `7f237ef` | [x] |
| 09 | mqadmin: `RabbitAdminService` | `8a01276` | [x] |
| 10 | mqadmin: REST + validación | `769499d` | [x] |
| 11 | mqadmin: seguridad, BFF e infra | `b076b7d` | [x] |
| 12 | frontend: panel de mensajería | `b22784a` | [x] |
| 13 | pruebas (ACK/DLQ, admin, validación) | `ed89e23` | [x] |
| 14 | documentación y entrega | (commit de cierre) | [x] |

Las tareas 00–07 ya estaban en `origin/feature/ep3-rabbitmq`; los commits de las
tareas 08–14 están **sin pushear**.

---

## 2. Decisiones tomadas (y por qué)

| Decisión | Motivo | Dónde queda documentada |
|----------|--------|-------------------------|
| Spring Boot **3.3.4** + Java 21 en todos los servicios | Coherente con la línea AMQP 3.x; evita migrar a Spring Boot 4 en medio de la EP | `fase-01` |
| Credenciales del broker `andesstay` / `AndesStay2024!`, vhost `andesstay` | El broker del `compose` **no tiene usuario `guest`**, que es el default de Spring Boot | `application.yml` de notify y reservations |
| Topología declarada **una sola vez**, en `notify` (tarea 03) | Un único dueño evita dos servicios compitiendo por la misma definición | `fase-03`, `fase-07` |
| Topología dividida por dominio (`NotificationsTopologyConfig`, `HousekeepingTopologyConfig`, `DocumentsTopologyConfig` + `TopologySupport`) | Refleja la separación de responsabilidades que pide la pauta y evita una clase gigante | `fase-03` |
| Ningún nombre de cola/exchange/routing key como literal en Java | Indicador 1 de la pauta | Javadoc de `MessagingProperties` (notify) y `MessagingProperties` (reservations) |
| Publishers por dominio sobre una base común (`AbstractMessagingPublisher` + `NotificationPublisher`, `HousekeepingTicketPublisher`, `DocumentPublisher`) | El controlador no conoce AMQP; la base concentra lo único "de mensajería" | `fase-04` |
| Validación Jakarta en el DTO + `@RestControllerAdvice` → **400** con `fields` por campo en notify | Evita publicar payloads inválidos; es un precedente del indicador 8 (que se formaliza en 10 y 13) | `fase-04` |
| **ACK manual** con reintentos *dentro* del listener y descarte con `basicNack(requeue=false)` | En `AcknowledgeMode.MANUAL` un `RepublishMessageRecoverer` del contenedor republicaría mientras el ACK sigue pendiente del listener → el original quedaría sin confirmar y se reentregaría (duplicados). Los reintentos quedan explícitos y acotados (3 intentos / 2 s) y el descarte lo resuelve el `x-dead-letter-exchange` de la cola | Javadoc de `AbstractDlqListener`, `fase-05` |
| `requeue=false` **siempre** | Elimina el bucle clásico de reentrega infinita | `fase-05` |
| `DeadLetterListener` **activo** que registra y confirma el descarte | El enunciado de la tarea 06 lo pide explícitamente. Contrapartida asumida: la DLQ no acumula mensajes, la evidencia queda en los logs | `fase-06` (y nota de revisión en `fase-05`) |
| Publicación **asíncrona** (`CompletableFuture`) + `connection-timeout` de 2 s en reservations | Con el default de 60 s, un envío síncrono con el broker caído retendría el hilo de la petición: el DoD "no bloquea" no se cumpliría | `fase-07` |
| `MessagingProperties` **duplicado por microservicio** (no módulo común) | Los microservicios no comparten artefacto; sólo se duplican nombres de configuración, no lógica | Javadoc de la clase en reservations, `fase-07` |
| `spring-boot-starter-oauth2-resource-server` **no** se añade en la tarea 08 | Trae `spring-boot-starter-security`, que por defecto protege todos los endpoints: `/actuator/health` daría 401 y el DoD pide `UP`. Se añade en la tarea 11 con su configuración | este documento (§4) |

---

## 3. Evidencia de verificación (resumen)

Cada tarea tiene su tabla de evidencia; esto es el resumen:

- **Build:** los 6 módulos compilan con `BUILD SUCCESS`, 0 errores, 0 warnings tras cada tarea.
- **Topología (tarea 03):** 6 colas durables (las 5 de negocio + la DLQ); las 5 de negocio con `x-dead-letter-exchange`, más 2 exchanges propios (`ex.andesstay.notifications`, `ex.andesstay.dlx`) y 6 bindings, en el vhost `andesstay`. Verificado con la Management API: `colas=6 durables=6 conDLX=5`, `bindings propios=6`.
- **ACK manual (05):** `INFO [ACK] canal=… deliveryTag=… reentregado=false`; payload inválido → `ERROR [DESCARTE->DLQ]` **sin** reintentos; fallo transitorio → 2 `WARN [REINTENTO]` (2 s entre ellos) y descarte; mensajes en la DLQ con headers `x-death` (`queue`, `routing-keys`, `reason=rejected`); colas de origen y no confirmados en 0.
- **Dominios (06):** cada ACK lo emite la clase de su dominio (`EmailListener`, `SmsListener`, `PushListener`, `HousekeepingTicketListener`, `DocumentGenerationListener`, `DeadLetterListener`); la lógica de negocio vive en `NotificationService`, `HousekeepingService` y `DocumentService`.
- **End-to-end real (07):** reserva #13 creada → `VOUCHER` consumido; `CONFIRMADA` → `FACTURA`; `CHECKOUT` → ticket de housekeeping, los tres con ACK. Las 6 colas quedan en 0.
- **Resiliencia (07):** con el broker inalcanzable el endpoint responde `201` en 1.91 s (vs 2.06 s con broker) y registra `WARN … ConnectException` sin afectar el core.
- **Sin literales de topología:** grep sobre `src/main/java` (excluyendo comentarios) → 0 coincidencias en notify y en reservations, incluido `ReservationService`.

**Sesión 2 (tareas 08–14):**

- **Build:** los **7 módulos** compilan con `BUILD SUCCESS` (JDK 21): audit, bff, catalog, mqadmin, notify, report, reservations. Frontend `tsc -b && vite build` OK.
- **Pruebas (13):** `notify` 7 tests y `mqadmin` 22 tests en verde, **sin broker** (Mockito para `Channel` y `AmqpAdmin`). `BUILD SUCCESS` en ambos.
- **mqadmin sin token:** `POST /api/mqadmin/queues` → 401; `/actuator/health` permitido (503 con el broker ausente). Validación verificada por curl: 400 (nombre/`amq.`/tipo), 404 (recurso no administrado), 502 (broker inalcanzable).
- **JDK del sistema = 26.0.1:** rompe Lombok en `bff`/`reservations` (`ExceptionInInitializerError: … TypeTag :: UNKNOWN`). La sesión 2 compiló con un **Temurin JDK 21 portátil** en `%TEMP%\jdk21\jdk-21.0.12.1+1` (`$env:JAVA_HOME` antes de `mvnw.cmd`).

---

## 4. Pendientes y decisiones abiertas

1. **Push + merge a `main`:** los commits de las tareas 08–14 están sin pushear; el merge a `main` es la decisión de entrega pendiente (ver §5).
2. **Prueba de humo end-to-end sin ejecutar:** la sesión 2 no tenía Docker ni broker, así que no se corrió el flujo reserva → documentos → housekeeping → DLQ. La secuencia exacta (con comandos) queda en `docs/MENSAJERIA_RABBITMQ.md`; las partes verificables sin broker están cubiertas por las pruebas de la tarea 13.
3. **Secretos en archivos versionados** (riesgo **R2**, `context.md` §10.3): hoy la contraseña de Supabase y las de RabbitMQ están en `application.properties`/`compose.yml` versionados. El usuario lo conocía y no se decidió cambiarlo en esta sesión.
4. **`oauth2-resource-server` en mqadmin:** se añadió en la tarea 11 (ver §2); `/actuator/health` sigue permitido y el BFF reenvía el Bearer.
5. **Nodos del broker local no clusterizados:** `localhost:5672` (rabbitmq-1) y `localhost:5673` (rabbitmq-2) escuchan ambos, pero sólo el primero tiene la topología. Publicar contra 5673 pierde el mensaje **en silencio** (AMQP no devuelve error al productor sin *publisher returns*). Es una limitación del entorno local, no del código; queda como recomendación para la entrega (usar un nodo, o habilitar `mandatory` + *publisher confirms*).
6. **Datos de prueba en Supabase:** la verificación de la tarea 07 creó reservas reales (`id` 9 a 13, huéspedes `huesped@`, `sinbroker@`, `bench@` y `final@andesstay.cl`).
7. **Tareas 02–06 dependen de la DB solo indirectamente:** `notify` no usa la base; `reservations` sí (Supabase). La base estuvo caída al comienzo de la sesión (`FATAL: tenant/user … not found`) y quedó operativa durante la tarea 07.

---

## 5. Próximo paso: entrega (tareas 08–14 **completadas**)

Las tareas 08–14 quedaron ejecutadas y commiteadas en `feature/ep3-rabbitmq`. Lo que falta es la entrega:

1. `git push origin feature/ep3-rabbitmq` (sube los commits de las tareas 08–14).
2. **Merge a `main`** (decisión del equipo) y obtener los enlaces de GitHub para AVA.
3. **Prueba de humo con broker** (opcional, si hay Docker): seguir `docs/MENSAJERIA_RABBITMQ.md` → *Guía de prueba end-to-end*.
4. **Decisión sobre secretos (R2):** externalizar/rotar las credenciales de RabbitMQ y Supabase (hoy con *fallback* versionado).

Ya no hay tareas de la EP3 pendientes en `tareas/README.md` (00–14 en `[x]`).

---

## 6. Cómo reproducir lo verificado

```powershell
# Broker local (vhost andesstay, usuario andesstay)
docker compose -f infra/mq/compose.yml up -d
# UI: http://localhost:15672  (andesstay / AndesStay2024!)

# Compilar (por módulo; cada uno es standalone)
cd Backend-api/ms-andesstay-notify;      .\mvnw.cmd -B clean package -DskipTests
cd Backend-api/ms-andesstay-reservations; .\mvnw.cmd -B clean package -DskipTests

# Arrancar
java -jar Backend-api/ms-andesstay-notify/target/ms-andesstay-notify-*.jar        # 8083
java -jar Backend-api/ms-andesstay-reservations/target/ms-andesstay-reservations-*.jar  # 8081

# Flujo productor -> consumidor
curl -X POST http://localhost:8081/api/reservations -H "Content-Type: application/json" `
  -d '{"guestEmail":"x@andesstay.cl","unitId":77,"checkInDate":"2026-12-10","checkOutDate":"2026-12-12"}'
curl -X PUT http://localhost:8081/api/reservations/<id>/status -H "Content-Type: application/json" -d '{"status":"CHECKOUT"}'
```

Notas de operación:

- Los scripts de verificación de esta sesión quedaron en `%TEMP%\opencode\tarea0X-*.ps1` (fuera del repo, efímeros); la evidencia está transcrita en cada archivo de tarea.
- **PowerShell 5.1:** al pasar JSON a `curl.exe` hay que enviarlo desde un archivo (`Set-Content -Encoding ASCII` + `--data-binary "@archivo"`), porque las comillas internas se pierden y el body llega malformado (400).
- **Procesos:** `Start-Process -PassThru -RedirectStandardOutput` devuelve un PID intermedio, no el de `java`; para cerrar bien hay que usar `taskkill /T /F /PID` o buscar por línea de comandos.
- **Colas limpias al cierre:** las 6 colas del vhost `andesstay` quedan en 0 mensajes (se purgaron tras cada prueba).
