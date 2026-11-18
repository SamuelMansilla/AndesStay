# Fase 8 — Pruebas, documentación y entrega

> **Estado:** pendiente · **Indicadores:** todas · **Depende de:** 06 · **Est.:** 0,5 d

## Objetivo

Cerrar la EP5 con evidencia reproducible: pruebas verdes, documentación actualizada y criterios de aceptación cumplidos.

## Pasos

1. **Pruebas:**
   - `KafkaAdminServiceTest`, `ReservationEventPublisherTest`, tests de validación/idempotencia/DLT.
   - `mvnw.cmd test` en los servicios modificados.
   - `npm run build` en `Frontend/`.
2. **Documentación:**
   - `docs/KAFKA_STREAMING.md`: topología, tópicos/configs, consumer groups, DLT, idempotencia, runbook, endpoints admin, prueba de lag/retención.
   - Actualizar [`docs/contexto.md`](../../docs/contexto.md): puerto 8087, nuevo admin, estado Kafka, riesgos.
   - Actualizar `README.md` (arquitectura, inicio rápido, docs).
   - Registrar la autorización de React para la entrega.
3. **Verificación final:**
   - Flujo completo: reserva → tópicos → report/audit → KPIs/timeline.
   - DLT: payload inválido → reintentos → `.DLT`.
   - Admin: crear/alterar/borrar tópico; listar grupos y lag.
   - RabbitMQ EP3 sigue funcionando.
4. **Limpieza:** `.gitignore`, sin `target/`, sin secretos nuevos.
5. **Entrega:** commit/tag y enlaces de GitHub.

## Definition of Done

- [ ] Tests y build verdes.
- [ ] Documentación completa y enlazada.
- [ ] Criterios de aceptación del plan marcados.
- [ ] Sin regresiones en RabbitMQ.
- [ ] Repositorio listo para AVA.

## Verificación

```powershell
# En cada servicio modificado
./mvnw.cmd -q test
# Frontend
cd Frontend; npm run build
# Git
git status --short
```

## Commit sugerido

```text
docs(repo): documenta streaming Kafka y cierra la EP5
```

## Rollback

`git revert <hash>`; la documentación no afecta el runtime.
