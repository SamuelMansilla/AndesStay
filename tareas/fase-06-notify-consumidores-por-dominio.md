# Tarea 06 — notify: consumidores agrupados por dominio funcional

- **Fase / Nº:** 06
- **Indicador(es) de la pauta:** **Indicador 4 (15%)** — consumidores `@RabbitListener` agrupados por dominio
- **Depende de:** 04, 05
- **Rama:** `feature/ep3-rabbitmq`
- **Commit sugerido:** `refactor(notify): reorganizar consumidores por dominio funcional`
- **Riesgo de rollback:** bajo

## Objetivo
Reorganizar los `@RabbitListener` en paquetes por **dominio funcional** (notifications, housekeeping, documents, dlq) para que la estructura del código refleje la separación de responsabilidades que pide la pauta.

## Alcance
**Incluye**
- Un paquete por dominio bajo `messaging/`.
- Listeners separados: email, sms, push (notifications); tickets (housekeeping); documentos (documents); DLQ (dlq).
- Cada listener aplica el patrón ACK/DLQ de la tarea 05.

**NO incluye**
- La lógica del broker (ya resuelta en 05).
- Pruebas (tarea 13).

## Prerrequisitos
- Tareas 04 y 05 completadas.

## Pasos
1. Reestructurar:
   ```
   messaging/
     notifications/  EmailListener, SmsListener, PushListener
     housekeeping/   HousekeepingTicketListener
     documents/      DocumentGenerationListener
     dlq/            DeadLetterListener
   ```
2. Mover la lógica de negocio de cada canal a un servicio (`NotificationService.sendEmail()`, etc.) y dejar el listener **delgado** (parsear → delegar → ACK).
3. `DeadLetterListener`: consume `q.andesstay.dlq`, registra en WARN el evento y sus headers (`x-death`, `x-original-queue`, `x-exception-message`); no reencola.
4. Añadir comentario de cabecera en cada listener indicando: dominio, cola, routing key y decisión de ACK.
5. Eliminar `NotificationListener` monolítico.

## Archivos a crear / modificar
| Acción | Archivo |
|--------|---------|
| Crear | `.../messaging/notifications/{Email,Sms,Push}Listener.java` |
| Crear | `.../messaging/housekeeping/HousekeepingTicketListener.java` |
| Crear | `.../messaging/documents/DocumentGenerationListener.java` |
| Crear | `.../messaging/dlq/DeadLetterListener.java` |
| Crear | `.../service/NotificationService.java`, `HousekeepingService.java`, `DocumentService.java` |
| Eliminar | `.../service/NotificationListener.java` |

## Criterios de aceptación (Definition of Done)
- [x] Existe **un paquete por dominio funcional**, con sus listeners.
- [x] Ningún paquete mezcla dominios.
- [x] Todos los consumidores usan `@RabbitListener` y el patrón ACK.
- [x] El listener de DLQ registra los mensajes fallidos.
- [x] Compila y arranca; el envío end-to-end sigue funcionando.

## Cómo verificar
```bash
cd Backend-api/ms-andesstay-notify
.\mvnw.cmd -q clean package -DskipTests
.\mvnw.cmd spring-boot:run
# Enviar por cada canal y observar logs por dominio
```

## Rollback
```bash
git revert <hash>
```
> Reorganización de paquetes; revertir no afecta la topología del broker.

## Notas
- La agrupación por dominio es lo que evalúa explícitamente el indicador 4: nombrar paquetes de forma autoexplicativa (`notifications`, `housekeeping`, `documents`, `dlq`).

---

## Resultado — 2026-10-15

### Estructura final (un paquete por dominio, y nada más)

```text
messaging/
  AbstractDlqListener.java          contrato ACK/NACK común (tarea 05)
  AbstractMessagingPublisher.java   base de los publishers (tarea 04)
  exception/                        RecoverableException, NonRecoverableException
  notifications/                    NotificationPublisher, EmailListener, SmsListener, PushListener
  housekeeping/                     HousekeepingTicketPublisher, HousekeepingTicketListener
  documents/                        DocumentPublisher, DocumentGenerationListener
  dlq/                              DeadLetterListener
service/
  NotificationService.java          envío por canal (email/sms/push)
  HousekeepingService.java          registro del ticket de aseo
  DocumentService.java              generación de voucher/factura
```

Cada dominio agrupa **su** publisher y **su** listener; ningún paquete mezcla dominios. Se eliminó el `NotificationListener` monolítico de `service/` (tenía los tres canales y el listener de la DLQ en una sola clase, mezclando dominios).

| Dominio | Listener (paquete) | Cola | Servicio de negocio |
|---------|--------------------|------|---------------------|
| notifications | `EmailListener` | `q.notify.email` | `NotificationService.sendEmail()` |
| notifications | `SmsListener` | `q.notify.sms` | `NotificationService.sendSms()` |
| notifications | `PushListener` | `q.notify.push` | `NotificationService.sendPush()` |
| housekeeping | `HousekeepingTicketListener` | `q.housekeeping.tickets` | `HousekeepingService.createTicket()` |
| documents | `DocumentGenerationListener` | `q.docs.generate` | `DocumentService.generateDocument()` |
| dlq | `DeadLetterListener` | `q.andesstay.dlq` | — (supervisión) |

### Listeners delgados

Cada listener hace sólo tres cosas: **recibir** (`@RabbitListener`), **delegar** en su servicio y **confirmar** (`handle(...)` → `AbstractDlqListener`, tarea 05). Toda la lógica de negocio —validación del payload, simulación de fallo transitorio y registro del envío— vive en los servicios, que no conocen RabbitMQ. Cada clase lleva su comentario de cabecera con dominio, cola, routing key, decisión de ACK y productores.

Materializa el indicador 3 (lógica de negocio separada de la mensajería) y el indicador 4 (consumidores agrupados por dominio).

### `DeadLetterListener` (supervisión de la DLQ)

Consume `q.andesstay.dlq` y deja en **un solo lugar** la traza completa del descarte:

```text
WARN [DLQ] Mensaje descartado: colaOrigen=q.notify.email routingKeyOriginal=notify.email
     motivo=rejected reentregas=1 excepcionOriginal=- colaOriginal=- payload={…}
```

Los datos salen de los headers `x-death` (y, si existieran, de `x-original-queue` y `x-exception-message` que agrega el `RepublishMessageRecoverer`). Recibe el `Message` **crudo**, no un evento tipado, porque en esta cola conviven payloads de los tres dominios; así ningún descarte falla al deserializarse. Aplica el mismo contrato ACK (confirma tras registrarlo) y nunca reencola.

> Nota de decisión (revisa la 05): la tarea 05 había eliminado este consumidor para que la DLQ quedara como destino terminal. La tarea 06 lo pide explícitamente, así que se reintroduce en `messaging/dlq/`, con la contrapartida asumida: la DLQ **no acumula** mensajes (la evidencia queda en el log del descarte, que es lo que exige el indicador 5, y en el `WARN [DLQ]`).

### Ajuste del contrato base

`AbstractDlqListener.process(...)` ya no recibe el nombre del canal (cada listener pertenece a un único canal) y se agregó el gancho `describe(T)`, que las subclases pueden acortar para los logs — el `DeadLetterListener` lo usa para no volcar todo el `MessageProperties` de un mensaje crudo. También se agregó `toString()` a `TicketEvent` y `DocumentEvent` para que los logs de ACK/descarte muestren el payload de forma legible.

### Verificación

**1. Compilación:** `.\mvnw.cmd -B clean package -DskipTests` → `BUILD SUCCESS`, 0 `[ERROR]`, 0 `[WARNING]`.

**2. Sin referencias colgando:** grep de `NotificationListener` en todo el backend → **0 coincidencias**.

**3. Arranque:** `Started MsAndesstayNotifyApplication in 10.591 seconds`.

**4. Un mensaje por cada dominio y canal** (logs recortados; la columna "logger" es la clase que los emite):

| Emisor (logger) | Mensaje |
|-----------------|---------|
| `NotificationService` | `[EMAIL] Enviado a: email@andesstay.cl para la reserva #2001. Contenido: Prueba EMAIL` |
| `m.m.notifications.EmailListener` | `[ACK] canal=EMAIL deliveryTag=1 reentregado=false` |
| `NotificationService` | `[SMS] Enviado a: sms@andesstay.cl. Mensaje: Prueba SMS` |
| `m.m.notifications.SmsListener` | `[ACK] canal=SMS deliveryTag=1 reentregado=false` |
| `NotificationService` | `[PUSH] Enviada alerta a: push@andesstay.cl. Notificación: Prueba PUSH` |
| `m.m.notifications.PushListener` | `[ACK] canal=PUSH deliveryTag=1 reentregado=false` |
| `HousekeepingService` | `[HOUSEKEEPING] Ticket 104d4412… (LIMPIEZA) registrado para la reserva #2001 de la unidad #7` |
| `m.m.housekeeping.HousekeepingTicketListener` | `[ACK] canal=HOUSEKEEPING deliveryTag=1 reentregado=false evento=TicketEvent{…}` |
| `DocumentService` | `[DOCS] Documento eff5ac9e… (VOUCHER) generado para la reserva #2001 y enviado a email@andesstay.cl` |
| `m.m.documents.DocumentGenerationListener` | `[ACK] canal=DOCUMENTS deliveryTag=1 reentregado=false evento=DocumentEvent{…}` |

Cada ACK lo emite la clase del dominio correspondiente: el patrón de la tarea 05 se aplica en todos los consumidores.

**5. DLQ:** un payload inválido inyectado directo al exchange produjo
`ERROR [DESCARTE->DLQ]` en `EmailListener` (no recuperable, sin reintentos) y luego el
`WARN [DLQ] Mensaje descartado: colaOrigen=q.notify.email routingKeyOriginal=notify.email motivo=rejected reentregas=1`
en `DeadLetterListener`, con `[ACK] canal=DLQ`. El camino de reintentos (SMS con `retry@andesstay.cl`) mostró los dos `WARN [REINTENTO]` y su descarte, seguido también del `WARN [DLQ]`.

**6. Estado final del broker:** las 6 colas en 0 mensajes y 0 sin confirmar (la DLQ queda vacía porque su consumidor confirma tras registrar; es el comportamiento asumido y documentado).

### Rollback
```bash
git revert <hash>
```
Reorganización de paquetes: revertir no toca la topología del broker.

