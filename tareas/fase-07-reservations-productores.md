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
- [ ] Al hacer CHECKOUT se publica un mensaje en `q.housekeeping.tickets`.
- [ ] Al crear/confirmar una reserva se publica un mensaje en `q.docs.generate`.
- [ ] Si RabbitMQ no está disponible, el endpoint **responde igual** (no bloquea).
- [ ] No hay literales de topología en la lógica de negocio.
- [ ] Compila y arranca.

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
