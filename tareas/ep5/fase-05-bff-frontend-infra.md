# Fase 5 — BFF, frontend e infraestructura

> **Estado:** pendiente · **Indicadores:** 3 (6%), 9 (6%) · **Depende de:** 04 · **Est.:** 1,5 d

## Objetivo

Exponer el admin de Kafka a través del BFF, agregar la página `/streaming` y registrar el servicio en el compose de aplicaciones.

## Pasos

1. **BFF:**
   - `services.kafkaadmin.url=${SERVICES_KAFKAADMIN_URL:http://localhost:8087}`.
   - `BffKafkaAdminController` en `/api/kafkaadmin/**` con `WebClient`, propagando `Authorization: Bearer`.
   - Mismas rutas que el admin (topics, configs, partitions, consumer-groups, publish, load-test).
2. **Frontend:**
   - `src/api/kafkaAdminService.ts` con `axiosClient`.
   - `src/pages/StreamingAdminPage.tsx`: tópicos (nombre, particiones, RF, configs), crear/eliminar, editar retención/cleanup; consumer groups con estado, miembros y lag; botón de carga.
   - Tipos en `src/types/index.ts`; ruta `/streaming` solo `Admin`; link en `Navbar`.
   - Reutilizar estilos `--as-*` existentes.
3. **Infra:**
   - `infra/apps/compose.yml`: `kafkaadmin-svc` (8087), `SERVICES_KAFKAADMIN_URL`, `KAFKA_BOOTSTRAP_SERVERS`, `AZURE_*`.
   - `bff-svc`: `SERVICES_KAFKAADMIN_URL=http://kafkaadmin-svc:8087`.
   - `KAFKA_ENABLED=true` en el entorno de demo.

## Definition of Done

- [ ] `/api/kafkaadmin/**` responde a través del BFF con Bearer.
- [ ] `/streaming` es visible y operable solo con rol Admin.
- [ ] `kafkaadmin-svc` está en el compose y arranca.
- [ ] `npm run build` pasa.

## Verificación

```powershell
cd Frontend; npm run build
docker compose -f infra/apps/compose.yml config
```

## Commit sugerido

```text
feat(frontend): agrega panel de administración de Kafka
```

## Rollback

`git revert <hash>`; el BFF y el frontend vuelven al estado EP3 sin afectar los consumidores.
