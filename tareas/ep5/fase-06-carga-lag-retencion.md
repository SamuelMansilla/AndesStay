# Fase 6 — Carga, lag y retención

> **Estado:** pendiente · **Indicadores:** 8 (12%), 7 (apoyo) · **Depende de:** 03, 05 · **Est.:** 1 d

## Objetivo

Demostrar con evidencia el escalamiento por particiones, el consumer lag y la retención configurada, tal como se vio en las guías 3.3.1 y 3.3.2.

## Pasos

1. **Carga sintética:**
   - `POST /api/kafkaadmin/load-test?count=N` (solo Admin) publica eventos válidos en `reservations.events` con keys distribuidas.
   - Log de throughput (`msg/s`) y duración.
2. **Lag controlado:**
   - Propiedad `andesstay.kafka.consumer.simulated-delay-ms` (default 0) en `report`/`audit`; en demo se levanta con 10–20 ms para que el consumidor sea más lento que el productor.
   - Medir lag en `GET /api/kafkaadmin/consumer-groups/{id}` y en Kafka UI.
3. **Escalamiento:**
   - Levantar 2–3 instancias de `report` con el mismo `group-id` y puertos distintos.
   - Verificar que las 3 particiones se reparten y que el lag baja más rápido.
4. **Retención:**
   - Documentar la prueba: `kafka-configs --alter --add-config retention.ms=300000` sobre un tópico de demo, esperar y verificar en Kafka UI; luego restaurar 7d/30d.
   - No cambiar la retención de producción en el compose.
5. **Evidencia:** capturas de Kafka UI (lag, particiones, consumo) y logs.

## Definition of Done

- [ ] El endpoint de carga funciona y reporta throughput.
- [ ] El lag es visible y baja al escalar consumidores.
- [ ] La retención se demuestra sin alterar la configuración de entrega.
- [ ] Capturas/logs agregados a `docs/`.

## Verificación

```powershell
# 100k eventos
curl -X POST "http://localhost:8080/api/kafkaadmin/load-test?count=100000" -H "Authorization: Bearer <token>"
# Kafka UI :9000 -> Consumers -> lag
```

## Commit sugerido

```text
test(kafka): agrega prueba de carga y análisis de consumer lag
```

## Rollback

`git revert <hash>`; la propiedad de delay queda en 0 y el endpoint de carga es inocuo.
