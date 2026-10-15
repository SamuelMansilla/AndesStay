# Tarea 04 — notify: publishers y separación config / negocio

- **Fase / Nº:** 04
- **Indicador(es) de la pauta:** **Indicador 3 (10%)** — separación de mensajería y lógica de negocio
- **Depende de:** 03
- **Rama:** `feature/ep3-rabbitmq`
- **Commit sugerido:** `refactor(notify): extraer publishers por dominio y limpiar NotifyController`
- **Riesgo de rollback:** bajo

## Objetivo
Sacar de los controladores y de la lógica de negocio cualquier detalle de RabbitMQ (routing keys, `convertAndSend`, exchange). Los controladores deben llamar a **servicios de publicación de alto nivel** agrupados por dominio.

## Alcance
**Incluye**
- `NotificationPublisher` (email/sms/push) usando `MessagingProperties`.
- `HousekeepingTicketPublisher` y `DocumentPublisher` (pueden compartir un `MessagingPublisher` base).
- Refactor de `NotifyController` para delegar y recibir un DTO validado.
- Introducir `TicketEvent` y `DocumentEvent` como modelos.

**NO incluye**
- Listeners (tarea 06).
- ACK manual (tarea 05).

## Prerrequisitos
- Tarea 03 completada (colas y bindings declarados).

## Pasos
1. Crear paquete `messaging/notifications/NotificationPublisher.java`:
   - Método `publish(NotificationEvent event)` que resuelve la routing key según el canal **usando `MessagingProperties`** (no `switch` con strings literales en el controller).
2. Crear `messaging/housekeeping/HousekeepingTicketPublisher.java` y `messaging/documents/DocumentPublisher.java`.
3. Crear DTO de entrada `dto/SendNotificationRequest.java` con campos del evento (sin `RabbitTemplate`).
4. Refactorizar `NotifyController`:
   - Inyecta `NotificationPublisher` (no `RabbitTemplate` ni `@Value`).
   - `POST /api/notify/send` → valida el DTO y llama `publisher.publish(...)`.
   - Mensajes de error amigables.
5. (Opcional) Añadir endpoints de prueba para tickets/documentos: `POST /api/notify/tickets`, `POST /api/notify/documents` que usen sus publishers.
6. Verificar que ningún controlador importe `org.springframework.amqp.*`.

## Archivos a crear / modificar
| Acción | Archivo |
|--------|---------|
| Crear | `.../notify/messaging/notifications/NotificationPublisher.java` |
| Crear | `.../notify/messaging/housekeeping/HousekeepingTicketPublisher.java` |
| Crear | `.../notify/messaging/documents/DocumentPublisher.java` |
| Crear | `.../notify/dto/SendNotificationRequest.java` |
| Crear | `.../notify/model/TicketEvent.java`, `.../notify/model/DocumentEvent.java` |
| Modificar | `.../notify/controller/NotifyController.java` |

## Criterios de aceptación (Definition of Done)
- [x] Ningún controlador importa ni usa `RabbitTemplate`, `AmqpTemplate` ni `@Value` de topología.
- [x] Las routing keys se resuelven desde `MessagingProperties`.
- [x] Existen publishers por dominio (notificaciones, housekeeping, documentos).
- [x] `POST /api/notify/send` sigue funcionando end-to-end.
- [x] Compila y arranca.
- [x] DTO de entrada validado con Jakarta Validation y errores 400 con detalle por campo.

## Cómo verificar
```bash
# 1. Levantar notify
cd Backend-api/ms-andesstay-notify && .\mvnw.cmd spring-boot:run
# 2. Enviar notificación
curl -X POST http://localhost:8083/api/notify/send -H "Content-Type: application/json" \
  -d '{"reservationId":1,"recipient":"test@andesstay.cl","channel":"EMAIL","subject":"Hola","body":"Prueba"}'
# 3. Sin lógica de mensajería en controladores
Select-String -Path src/main/java/**/controller/*.java -Pattern "RabbitTemplate|convertAndSend|amqp"
```

## Rollback
```bash
git revert <hash>
```
> Si se revierte, el ruteo inline vuelve al controlador (versión previa). Sin efectos en el broker.

## Notas
- Este es el cambio que más directamente evidencia la **separación de responsabilidades** ante la pauta: dejarlo bien comentado.
- Un `MessagingPublisher` genérico que reciba `(exchange, routingKey, payload)` puede reducir duplicación, pero mantener un publisher por dominio mejora la legibilidad del indicador 3.

---

## Resultado — 2026-10-15

### Patrón aplicado (decisión del equipo): **base abstracta + un publisher por dominio**

`AbstractMessagingPublisher` concentra lo único que es "de mensajería": el `AmqpTemplate` y el exchange principal. Cada dominio expone métodos de alto nivel y resuelve su routing key desde `MessagingProperties`.

| Acción | Archivo | Rol |
|--------|---------|-----|
| Crear | `messaging/AbstractMessagingPublisher.java` | base: `publish(routingKey, payload)` con exchange desde configuración |
| Crear | `messaging/notifications/NotificationPublisher.java` | `publish(NotificationEvent)` → routing key según canal (EMAIL/SMS/PUSH) |
| Crear | `messaging/housekeeping/HousekeepingTicketPublisher.java` | `publish(TicketEvent)` → `housekeeping.ticket` |
| Crear | `messaging/documents/DocumentPublisher.java` | `publish(DocumentEvent)` → `docs.generate` |
| Crear | `dto/SendNotificationRequest.java` | DTO de entrada con validaciones Jakarta |
| Crear | `model/TicketEvent.java`, `model/DocumentEvent.java` | modelos publicados por RabbitMQ |
| Crear | `exception/NotifyExceptionHandler.java` | `@RestControllerAdvice` → 400 con `fields` |
| Modificar | `controller/NotifyController.java` | sólo valida y delega en los publishers |
| Modificar | `pom.xml` | `spring-boot-starter-validation` |

### Antes / después en el controlador

| Antes | Después |
|-------|---------|
| el controlador construía el `switch` de canal con literales `"notify.email"`, etc. | el canal se resuelve en `NotificationPublisher` desde `MessagingProperties` |
| inyectaba `RabbitTemplate` y hacía `convertAndSend(exchange, routingKey, event)` | inyecta `NotificationPublisher`, `HousekeepingTicketPublisher`, `DocumentPublisher` |
| devolvía el routing key que él mismo calculaba | devuelve el routing key que le entrega el publisher |

El controlador ya no importa `org.springframework.amqp.*` (verificado con grep, ver más abajo).

### Validación (apoyo al indicador 8, que se formaliza en las tareas 10 y 13)

`SendNotificationRequest`: `@NotNull @Positive reservationId`, `@NotBlank @Size(max=200) recipient`, `@NotBlank @Pattern((?i)EMAIL|SMS|PUSH) channel`, `@Size(max=200) subject`, `@NotBlank body`.

`NotifyExceptionHandler` responde **400** con el detalle por campo:

```json
{"timestamp":"2026-10-15T20:19:24.453Z","status":400,"error":"Solicitud inválida",
 "fields":{"channel":"channel debe ser EMAIL, SMS o PUSH",
           "reservationId":"reservationId debe ser positivo",
           "body":"body es obligatorio",
           "recipient":"recipient es obligatorio"}}
```

Un payload inválido se rechaza **antes** de publicar, así que nunca llega a la cola (aprovechable por la tarea 05 para los casos no recuperables).

### Endpoints añadidos (paso 5, opcional pero adoptado)

`POST /api/notify/tickets` y `POST /api/notify/documents`: permiten probar los publishers de housekeeping y documents sin depender de `reservations` (que será el productor real en la tarea 07).

### Verificación

**1. Compilación:** `.\mvnw.cmd -B package -DskipTests` → `BUILD SUCCESS`, 0 `[ERROR]`, 0 `[WARNING]`.

**2. Controladores sin mensajería:** grep de `RabbitTemplate|AmqpTemplate|convertAndSend|org\.springframework\.amqp|@Value` sobre `*/controller/*.java`, excluyendo líneas de comentario → **0 coincidencias**.

**3. Arranque:** `Started MsAndesstayNotifyApplication in 10.504 seconds` (`8083`).

**4. Pruebas HTTP:**

| Caso | Resultado |
|------|-----------|
| `POST /send` válido (EMAIL, reserva 777) | `200` → `Notificación enviada a RabbitMQ con routingKey: notify.email` |
| Consumo del mensaje | `[EMAIL] Enviado a: valido@andesstay.cl para la reserva #777. Contenido: Cuerpo valido` |
| `POST /send` inválido (recipient vacío, canal `TELEGRAM`, body vacío, reserva `-1`) | `400` con los 4 campos en `fields` |
| `POST /tickets` válido | `200`; `q.housekeeping.tickets` con **1** mensaje |
| `POST /documents` válido | `200`; `q.docs.generate` con **1** mensaje |
| `q.andesstay.dlq` | **0** mensajes (nada se descartó) |

Los mensajes de tickets/documentos quedaban en cola porque aún no tienen consumidor (tarea 06); **se purgaron** tras la prueba para dejar el broker limpio (confirmado: las colas quedaron en 0).

> La profundidad 1 es la evidencia de que la routing key resuelta desde configuración enruta correctamente: con una routing key equivocada el mensaje se descartaría y la cola quedaría en 0.

### Rollback
```bash
git revert <hash>
```
Sin cambios de topología ni de broker; sólo vuelve el `switch` al controlador.

