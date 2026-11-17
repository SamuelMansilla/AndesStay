# Fase 3 — Consumidores, validación, idempotencia y DLT

> **Estado:** pendiente · **Indicadores:** 5 (14%), 6 (12%), 8 (apoyo) · **Depende de:** 02 · **Est.:** 2 d

## Objetivo

Separar los listeners de la lógica de dominio, consumir DTOs validados y garantizar que un mensaje defectuoso no detenga el pipeline: reintentos acotados, DLT y efecto idempotente.

## Pasos

1. **Separación de capas:**
   - `report`: mover `ReservationEventListener` a `messaging/`; el listener valida y delega en `ReportService`.
   - `audit`: mover el método `@KafkaListener` a `messaging/AuditEventListener`; delega en `AuditService`.
   - Los servicios de dominio no conocen `ConsumerRecord` ni `KafkaTemplate`.
2. **Validación:**
   - DTO con `@NotNull`, `@NotBlank`, `@Positive`, `@Email` según campo.
   - Configurar `Validator` en el contenedor (`KafkaListenerConfigurer` + `LocalValidatorFactoryBean`) y `@Valid` en el parámetro del listener.
   - `eventId`, `reservationId`, `eventType`, `status` obligatorios.
3. **Idempotencia:**
   - Tabla `processed_messages` (`message_id` PK, `topic`, `partition`, `offset`, `processed_at`).
   - Insertar el `eventId` y ejecutar la lógica en la misma transacción; si hay duplicado, `skip` con log `INFO`.
4. **Errores y DLT:**
   - `ErrorHandlingDeserializer` para payloads corruptos.
   - `DefaultErrorHandler` con `ExponentialBackOffWithMaxRetries(2)` (1s/2s/4s) y `DeadLetterPublishingRecoverer` a `<topic>.DLT`.
   - `commitRecovered=true`; log `WARN` en reintento y `ERROR` en descarte con tópico/partición/offset/key.
   - Excepción no reintentable para errores de negocio permanentes.
5. **Observabilidad:**
   - `RecordInterceptor` que lee `X-Trace-Id`/`X-Event-Id` y los pone en MDC.
   - Formato de log con `traceId`, `topic`, `partition`, `offset`.
6. **Tests:**
   - Payload inválido → no llega al servicio.
   - Duplicado → no duplica el efecto.
   - Fallo transitorio → reintentos → DLT (unitario o `@EmbeddedKafka`).

## Definition of Done

- [ ] Listeners en `messaging/`, lógica en `service/`.
- [ ] `@Valid` efectivo y errores de validación registrados.
- [ ] DLT recibe el mensaje tras agotar reintentos.
- [ ] `processed_messages` evita duplicados.
- [ ] Logs con contexto de tópico/partición/offset/traceId.
- [ ] Tests de validación, idempotencia y DLT pasan.

## Verificación

```powershell
cd Backend-api/ms-andesstay-report; ./mvnw.cmd -q test
cd ../ms-andesstay-audit;            ./mvnw.cmd -q test
# Con clúster: publicar payload inválido y ver *.DLT en Kafka UI
```

## Commit sugerido

```text
feat(kafka): agrega consumidores validados, idempotencia y DLT
```

## Rollback

`git revert <hash>`; deshabilitar la tabla `processed_messages` no rompe el flujo (se ignora si no existe).
