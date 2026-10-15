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
- [ ] Ningún controlador importa ni usa `RabbitTemplate`, `AmqpTemplate` ni `@Value` de topología.
- [ ] Las routing keys se resuelven desde `MessagingProperties`.
- [ ] Existen publishers por dominio (notificaciones, housekeeping, documentos).
- [ ] `POST /api/notify/send` sigue funcionando end-to-end.
- [ ] Compila y arranca.

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
