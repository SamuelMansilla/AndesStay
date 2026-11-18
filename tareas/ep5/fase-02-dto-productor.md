# Fase 2 — DTO y productor tipado

> **Estado:** pendiente · **Indicadores:** 2 (12%), 4 (14%) · **Depende de:** 01 · **Est.:** 1 d

## Objetivo

Reemplazar el JSON armado con `String.format` por un DTO tipado publicado con `JsonSerializer`, manteniendo el envelope actual y la regla de no bloquear el core.

## Pasos

1. Crear `ReservationEventDTO` (y copia de contrato en `report`/`audit`):
   - `eventId`, `eventType`, `type`, `timestamp`, `createdAt`, `traceId`, `correlationId`, `actor`, `reservationId`, `unitId`, `status`, `details`.
   - `@JsonIgnoreProperties(ignoreUnknown = true)`; sin lógica de negocio.
2. Refactorizar `ReservationEventPublisher`:
   - Construir el DTO con `ObjectMapper`/builder, no con `String.format`.
   - `KafkaTemplate<String, ReservationEventDTO>`; `key = reservationId`.
   - Enviar a `reservations.events` y `audit.timeline` leyendo nombres de propiedades.
   - Headers: `X-Event-Id`, `X-Trace-Id`, `X-Correlation-Id`.
   - `whenComplete`: log `INFO` con tópico/partición/offset y `WARN` con `eventId` si falla.
   - Mantener el fallback HTTP solo cuando Kafka está deshabilitado (`kafka.enabled=false`).
3. Añadir tests:
   - Serialización/deserialización del DTO (contrato JSON).
   - `ReservationEventPublisherTest` con `KafkaTemplate` mockeado: verifica topic, key y headers.

## Definition of Done

- [ ] No queda `String.format` para construir el payload.
- [ ] No quedan literales `"reservations.events"`/`"audit.timeline"` en Java.
- [ ] El productor envía a ambos tópicos con key y headers.
- [ ] El fallback HTTP sigue disponible solo con Kafka deshabilitado.
- [ ] Tests de contrato y de productor pasan.

## Verificación

```powershell
cd Backend-api/ms-andesstay-reservations
./mvnw.cmd -q -Dtest=ReservationEventPublisherTest test
```

## Commit sugerido

```text
feat(reservations): publica eventos Kafka con DTO tipado
```

## Rollback

`git revert <hash>`; el fallback HTTP y el DTO anterior se recuperan del commit previo.
