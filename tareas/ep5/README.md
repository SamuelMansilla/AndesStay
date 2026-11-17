# Plan de trabajo EP5 — Kafka (AndesStay · DSY1107)

Carpeta de **tareas atómicas** para incorporar streaming con Kafka cumpliendo la pauta de la Evaluación Parcial N°5.

> Contexto, decisiones y arquitectura: [`../../docs/PLAN_EP5_KAFKA.md`](../../docs/PLAN_EP5_KAFKA.md).
> Estado de la EP3 (RabbitMQ): [`../README.md`](../README.md) y [`../_estado-ep3.md`](../_estado-ep3.md).

---

## Flujo de trabajo

1. Elegir la siguiente tarea **no bloqueada** (respeta *Depende de*).
2. Implementar siguiendo los **Pasos** hasta cumplir el **Definition of Done**.
3. Correr la **Verificación**.
4. Commit único con el mensaje sugerido.
5. Marcar la tarea en este README.
6. Si algo falla, aplicar el **Rollback** de la tarea.

## Rama y commits

- Rama integradora: `feature/ep5-kafka` (un commit por tarea).
- Convención: `<tipo>(<ámbito>): <descripción en imperativo>`.
- Ámbitos: `kafka`, `reservations`, `report`, `audit`, `kafkaadmin`, `bff`, `frontend`, `infra`, `repo`.
- Nunca mezclar dos tareas en un commit.

## Índice

| # | Tarea | Indicador EP5 | Depende de | Est. | Estado |
|---|---|---|---|---|---|
| 00 | [Preparación](fase-00-preparacion.md) | — | — | 0,5 d | [x] |
| 01 | [Cliente Kafka y topología](fase-01-cliente-kafka-topologia.md) | 1, 7 | 00 | 1,5 d | [x] |
| 02 | [DTO y productor tipado](fase-02-dto-productor.md) | 2, 4 | 01 | 1 d | [x] |
| 03 | [Consumidores, validación e idempotencia](fase-03-consumidores-errores-dlt.md) | 5, 6 | 02 | 2 d | [x] |
| 04 | [Microservicio kafkaadmin](fase-04-kafkaadmin.md) | 3, 9 | 01 | 2 d | [x] |
| 05 | [BFF, frontend e infraestructura](fase-05-bff-frontend-infra.md) | 3, 9 | 04 | 1,5 d | [x] |
| 06 | [Carga, lag y retención](fase-06-carga-lag-retencion.md) | 8 | 03, 05 | 1 d | [ ] |
| 07 | [Kafka Streams (opcional)](fase-07-kafka-streams-opcional.md) | valor agregado | 03 | 1,5 d | [ ] |
| 08 | [Pruebas, documentación y entrega](fase-08-pruebas-documentacion-entrega.md) | todas | 06 | 0,5 d | [ ] |

### Estado de la sesión (2026-11-17)

Implementado y verificado contra el clúster local:

- Productor tipado publicando en `reservations.events` y `audit.timeline` con key y headers.
- Consumidores de report/audit con DTO, `@Valid`, idempotencia y DLT.
- Payload corrupto enviado a `reservations.events.DLT` (verificado con `kafka-console-consumer`).
- `ms-andesstay-kafkaadmin` con endpoints de tópicos, configs, particiones, consumer groups y load-test; tests de integración en verde.
- BFF y página `/streaming` compilando (`npm run build`).
- **Verificación contra Supabase (2026-11-17):** conexión OK, tablas `report_processed_messages` y `audit_processed_messages` creadas por `ddl-auto=update`; reserva #21 y su confirmación persistidas y proyectadas en `report_reservation_kpis` y `audit_logs`; reenvío del mismo `eventId` ignorado por ambos consumidores (idempotencia).
- **Bug preexistente corregido:** el filtro de auditoría con parámetros nulos fallaba en Postgres (`lower(bytea)`); se corrigió con `CAST` y caminos rápidos por reserva/actor/tipo (`382b1df`).

Pendiente para cerrar: evidencia de lag/retención (fase 06), Kafka Streams opcional (07) y documentación/entrega final (08).

> Los avances se registran en la rama `feature/ep5-kafka` con **un commit por fase** (F0–F5) más el fix de auditoría.

**Total:** ~10 días efectivos, +1,5 si se incluye la fase 7.

## Cobertura de la pauta

| Indicador | Peso | Tareas |
|---|---:|---|
| Configuración completa del cliente Kafka | 12% | 01 |
| Productores y consumidores con DTO en ambos tópicos | 12% | 02, 03 |
| Admin: tópicos y configuraciones | 6% | 04, 05 |
| Productores Kafka | 14% | 02 |
| Consumidores `@KafkaListener` separados del dominio | 14% | 03 |
| Manejo de errores (validación, logging, reintentos, DLT) | 12% | 03 |
| Parámetros de tópicos (retención, cleanup, particiones, RF) | 12% | 01, 04 |
| Consumer groups | 12% | 03, 06 |
| Admin/scripts de tópicos y consumer groups | 6% | 04, 05, 06 |

## Definition of Done global

- `mvnw.cmd test` pasa en los servicios modificados.
- `npm run build` pasa en `Frontend/`.
- RabbitMQ (EP3) sigue funcionando.
- `docs/PLAN_EP5_KAFKA.md`, `docs/KAFKA_STREAMING.md` y `README.md` actualizados.
- Sin secretos nuevos versionados.
