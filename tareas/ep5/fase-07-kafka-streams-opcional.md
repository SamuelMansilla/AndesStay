# Fase 7 — Kafka Streams (opcional)

> **Estado:** opcional · **Indicadores:** valor agregado (3.1.2, 3.2.2) · **Depende de:** 03 · **Est.:** 1,5 d

## Objetivo

Agregar un pipeline de streaming con estado para métricas en tiempo real, como valor agregado frente a la rúbrica (no es requisito explícito de la EP5).

## Pasos

1. En `report`, agregar `kafka-streams` y `spring-kafka` (ya presente).
2. `@EnableKafkaStreams` + `StreamsBuilder`:
   - Source: `reservations.events` con `JsonSerde<ReservationEventDTO>`.
   - Filtrar eventos válidos (`reservationId` no nulo).
   - `groupBy((key, event) -> event.getUnitId())` + `windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))` + `count()`.
   - Sink: `report.metrics` (3P/RF3) con `JsonSerde`.
3. `application.id=andesstay-report-streams` (consumer group propio).
4. Crear `report.metrics` en Compose/admin.
5. Test con `TopologyTestDriver`.

## Definition of Done

- [ ] La topología compila y arranca con Kafka.
- [ ] Publica conteos por unidad en ventanas de 5 min.
- [ ] `report.metrics` existe con 3 particiones y RF3.
- [ ] Test de topología pasa.

## Verificación

```powershell
cd Backend-api/ms-andesstay-report
./mvnw.cmd -q -Dtest=ReportMetricsTopologyTest test
# Kafka UI -> report.metrics
```

## Commit sugerido

```text
feat(report): agrega topología Kafka Streams de métricas
```

## Rollback

`git revert <hash>`; el consumidor `report` sigue funcionando sin Streams.
