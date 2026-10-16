# Tarea 02 — notify: centralizar la topología RabbitMQ

- **Fase / Nº:** 02
- **Indicador(es) de la pauta:** **Indicador 1 (12%)** — nombres centralizados
- **Depende de:** 01
- **Rama:** `feature/ep3-rabbitmq`
- **Commit sugerido:** `feat(notify): centralizar topología RabbitMQ en MessagingProperties`
- **Riesgo de rollback:** bajo (solo configuración)

## Objetivo
Eliminar todo nombre de cola/exchange/binding/routing key escrito como literal en Java. Toda la topología debe leerse desde `application.yml` mediante una clase `@ConfigurationProperties`, en preparación para migrar a la convención `q.<dominio>.<acción>`.

## Alcance
**Incluye**
- Crear `MessagingProperties` (`@ConfigurationProperties(prefix="andesstay.messaging")`).
- Migrar `application.properties` → `application.yml` con el árbol `andesstay.messaging.*`.
- Añadir las claves de los nuevos casos de uso (tickets, documentos) aunque sus beans lleguen en la tarea 03.

**NO incluye**
- Declarar los beans `Queue`/`Exchange`/`Binding` (tarea 03).
- Cambiar el comportamiento de producers/consumers (tareas 04–06).

## Prerrequisitos
- Tarea 01 completada (`notify` compila).

## Pasos
1. Crear la convención de nombres en configuración, sustituyendo los actuales `q.cmd.notify.*`:
   - Exchange: `ex.andesstay.notifications`
   - Colas: `q.notify.email`, `q.notify.sms`, `q.notify.push`, `q.housekeeping.tickets`, `q.docs.generate`
   - DLX: `ex.andesstay.dlx` · DLQ: `q.andesstay.dlq` · key DLQ: `dlq.routing.key`
   - Routing keys: `notify.email`, `notify.sms`, `notify.push`, `housekeeping.ticket`, `docs.generate`
2. Crear `application.yml` (reemplaza `application.properties`) con:
   ```yaml
   spring:
     rabbitmq:
       host: ${RABBITMQ_HOST:localhost}
       port: ${RABBITMQ_PORT:5672}
       username: ${RABBITMQ_USER:guest}
       password: ${RABBITMQ_PASS:guest}
       listener:
         simple:
           retry: { enabled: true, initial-interval: 2000ms, max-attempts: 3 }
   andesstay:
     messaging:
       exchange: ex.andesstay.notifications
       dlx: ex.andesstay.dlx
       dlq: q.andesstay.dlq
       dlq-routing-key: dlq.routing.key
       queues:
         notify-email: q.notify.email
         notify-sms: q.notify.sms
         notify-push: q.notify.push
         housekeeping-tickets: q.housekeeping.tickets
         docs-generate: q.docs.generate
       routing-keys:
         notify-email: notify.email
         notify-sms: notify.sms
         notify-push: notify.push
         housekeeping-ticket: housekeeping.ticket
         docs-generate: docs.generate
   ```
3. Crear `config/MessagingProperties.java` con getters/setters (o `record` anidado) que mapee **todo** el árbol.
4. Añadir `@ConfigurationPropertiesScan` o `@EnableConfigurationProperties(MessagingProperties.class)` en `MsAndesstayNotifyApplication`.
5. Refactorizar `RabbitMQConfig` para inyectar `MessagingProperties` en lugar de `@Value` sueltos (los beans aún se definen en tarea 03; aquí solo se prepara la fuente de datos).

## Archivos a crear / modificar
| Acción | Archivo |
|--------|---------|
| Crear | `Backend-api/ms-andesstay-notify/src/main/resources/application.yml` |
| Crear | `.../notify/config/MessagingProperties.java` |
| Modificar | `.../notify/config/RabbitMQConfig.java` (usar `MessagingProperties`) |
| Modificar | `.../notify/MsAndesstayNotifyApplication.java` |
| Eliminar | `.../notify/src/main/resources/application.properties` |

## Criterios de aceptación (Definition of Done)
- [x] No existe ningún nombre de cola/exchange/routing key como **literal** en clases Java.
- [x] Toda la topología se lee de `andesstay.messaging.*`.
- [x] El servicio compila y arranca igual que antes.
- [x] Documentado (comentario en YAML) el significado de cada clave.

## Cómo verificar
```bash
cd Backend-api/ms-andesstay-notify
.\mvnw.cmd -q clean package -DskipTests
.\mvnw.cmd spring-boot:run
# Revisar en la UI de RabbitMQ que las colas existentes siguen declarándose.
# Buscar literales prohibidos:
Select-String -Path src/**/*.java -Pattern '"q\.|"ex\.|notify\.email'
```

## Rollback
```bash
git revert <hash>
```
> Bajo riesgo: solo cambia la fuente de los nombres, no la topología declarada.

## Notas
- Este cambio de nombres **rompe** la compatibilidad con colas ya creadas en el broker local. Como estamos en desarrollo, borrar las colas viejas en la UI o usar `-p` en compose (volumen limpio).
- Mantener ambos casos (viejos y nuevos) solo si hay despliegues productivos; en este proyecto no se requiere.

---

## Resultado — 2026-10-15

### Archivos

| Acción | Archivo |
|--------|---------|
| Crear | `src/main/resources/application.yml` |
| Crear | `src/main/java/com/duocuc/ms_andesstay_notify/config/MessagingProperties.java` |
| Modificar | `.../config/RabbitMQConfig.java` (inyecta `MessagingProperties`; ya no usa `@Value`) |
| Modificar | `.../controller/NotifyController.java` (sin literales de routing key ni `@Value`) |
| Modificar | `.../service/NotificationListener.java` (placeholders a las claves nuevas) |
| Modificar | `.../MsAndesstayNotifyApplication.java` (`@ConfigurationPropertiesScan`) |
| Eliminar | `src/main/resources/application.properties` |

### Topología centralizada (`andesstay.messaging.*`)

| Clave | Valor |
|-------|-------|
| `exchange` | `ex.andesstay.notifications` |
| `dlx` | `ex.andesstay.dlx` |
| `dlq` | `q.andesstay.dlq` |
| `dlq-routing-key` | `dlq.routing.key` |
| `queues.notify-email / -sms / -push` | `q.notify.email` / `q.notify.sms` / `q.notify.push` |
| `queues.housekeeping-tickets` | `q.housekeeping.tickets` (bean en tarea 03) |
| `queues.docs-generate` | `q.docs.generate` (bean en tarea 03) |
| `routing-keys.notify-email / -sms / -push` | `notify.email` / `notify.sms` / `notify.push` |
| `routing-keys.housekeeping-ticket` | `housekeeping.ticket` (tarea 07) |
| `routing-keys.docs-generate` | `docs.generate` (tarea 07) |

Todas las claves llevan **comentario explicativo en el YAML** (DoD).

### Conexión al broker (aclaración de alcance)
Los defaults del YAML apuntan al **cluster local del compose** (`localhost:5672`, usuario `andesstay`, vhost `andesstay`), parametrizados con `RABBITMQ_HOST / _PORT / _USER / _PASS / _VHOST`. El `guest/guest` original se descartó porque **el broker del compose no tiene usuario `guest`** (hallazgo 1 de la tarea 01): así el servicio arranca out-of-the-box en local y se sobreescribe en EC2.

### Alcance respetado
- **No** se declararon beans nuevos de tickets/documentos (tarea 03).
- **No** se cambió el modelo de ACK/DLQ (tarea 05).
- En `NotifyController` se eliminaron los literales de routing key y el `@Value` del exchange (exigido por el DoD de esta tarea), pero **la extracción a un `NotificationPublisher` dedicado sigue en la tarea 04** (indicador 3).

### Verificación

**1. Compilación:** `.\mvnw.cmd -B clean package -DskipTests` → `BUILD SUCCESS`, 0 `[ERROR]`, 0 `[WARNING]`.

**2. Sin literales de topología en Java:** grep sobre `src/main/java` con `'"q\.|"ex\.|notify\.(email|sms|push)|dlq\.routing\.key|andesstay\.rabbitmq'` → **0 coincidencias**.

**3. Arranque + prueba funcional end-to-end** (jar en `8083`, broker real):
```
POST /api/notify/send {channel:EMAIL} -> "Notificación enviada a RabbitMQ con routingKey: notify.email"
POST /api/notify/send {channel:SMS}   -> "... routingKey: notify.sms"
POST /api/notify/send {channel:PUSH}  -> "... routingKey: notify.push"

[EMAIL] Enviado a: email@andesstay.cl para la reserva #555. Contenido: Mensaje de prueba EMAIL
[SMS]   Enviado a: sms@andesstay.cl. Mensaje: Mensaje de prueba SMS
[PUSH]  Enviada alerta a: push@andesstay.cl. Notificación: Mensaje de prueba PUSH
```
Los 3 consumidores reciben desde las colas nuevas: comportamiento equivalente al anterior.

**4. Topología declarada en el broker** (Management API, vhost `andesstay`):

| Cola | durable | argumentos |
|------|---------|------------|
| `q.notify.email` | sí | `x-dead-letter-exchange=ex.andesstay.dlx`, `x-dead-letter-routing-key=dlq.routing.key` |
| `q.notify.sms` | sí | idem |
| `q.notify.push` | sí | idem |
| `q.andesstay.dlq` | sí | — |

Exchanges: `ex.andesstay.notifications` (direct, durable) y `ex.andesstay.dlx` (direct, durable).
Bindings: `ex.andesstay.notifications --[notify.email|sms|push]--> q.notify.*` y `ex.andesstay.dlx --[dlq.routing.key]--> q.andesstay.dlq`.

**5. Migración de nombres:** se eliminaron del broker las colas viejas `q.cmd.notify.email/sms/push/dlq` (quedaban del arranque previo). Estado final del vhost `andesstay`: solo las 4 colas nuevas.

### Rollback
```bash
git revert <hash>
```

