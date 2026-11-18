# Fase 4 — Microservicio kafkaadmin

> **Estado:** pendiente · **Indicadores:** 3 (6%), 9 (6%) · **Depende de:** 01 · **Est.:** 2 d

## Objetivo

Crear `ms-andesstay-kafkaadmin` (puerto 8087), un microservicio dedicado a administrar tópicos, configuraciones y consumer groups mediante `AdminClient`, con JWT y rol Admin.

## Pasos

1. **Esqueleto** (copiar patrón de `ms-andesstay-mqadmin`):
   - `pom.xml`: web, spring-kafka, validation, actuator, oauth2-resource-server, test.
   - `Dockerfile`, `application.yml` (puerto 8087, bootstrap, Azure issuer/audience).
   - `SecurityConfig` + `JwtRoleConverter` + validadores de issuer/audience.
   - `@PreAuthorize("hasRole('Admin')")` en el controller.
2. **`KafkaAdminService`** sobre `AdminClient`:
   - `listTopics` (particiones, RF, configs), `describeTopic`.
   - `createTopic(name, partitions, RF, configs)`, `alterConfigs`, `increasePartitions`, `deleteTopic`.
   - `listConsumerGroups`, `describeConsumerGroup` (miembros, asignación, offsets, lag = LEO − committed).
   - `deleteConsumerGroup` (solo inactivo).
   - `clusterInfo` (brokers, controller).
   - `publishTestMessage` y `loadTest(count)` (demo).
3. **Topología declarada:** `KafkaTopicsProperties` con los tópicos del plan (particiones, RF, retención, cleanup, min ISR) y `GET /topics/declared`.
4. **REST + validación:**
   - DTOs `CreateTopicRequest`, `AlterConfigsRequest`, `PublishRequest`, `LoadTestRequest`.
   - Patrón de nombre, rangos de particiones/RF y whitelist de configs.
   - `KafkaAdminExceptionHandler`: 400/404/409/502/504.
5. **Tests:** `KafkaAdminServiceTest` con `AdminClient` mockeado; validación de DTOs con `MockMvc`.

## Definition of Done

- [ ] El servicio arranca en 8087 y exige JWT con rol Admin.
- [ ] Endpoints de tópicos, configs, particiones y consumer groups funcionan.
- [ ] Lag por partición calculado correctamente.
- [ ] Validación de entrada con errores claros.
- [ ] Tests unitarios de servicio y validación pasan.

## Verificación

```powershell
cd Backend-api/ms-andesstay-kafkaadmin
./mvnw.cmd -q test
./mvnw.cmd spring-boot:run
# GET http://localhost:8087/api/kafkaadmin/topics con Bearer Admin
```

## Commit sugerido

```text
feat(kafkaadmin): agrega microservicio administrador de Kafka
```

## Rollback

Eliminar la carpeta del microservicio y su registro en Compose/BFF; no afecta a los productores/consumidores.
