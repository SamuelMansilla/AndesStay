# Tarea 11 — mqadmin: seguridad, BFF e infraestructura

- **Fase / Nº:** 11
- **Indicador(es) de la pauta:** **Indicador 6** + requisito "filtros que validen el JWT"
- **Depende de:** 10
- **Rama:** `feature/ep3-rabbitmq`
- **Commit sugerido:** `feat(mqadmin): asegurar API con JWT y exponerla a través del BFF`
- **Riesgo de rollback:** bajo

## Objetivo
Proteger el microservicio administrador con validación de JWT (Azure Entra ID) y exponerlo a través del **BFF**, además de integrarlo en la infraestructura (docker compose). Solo rol **Admin** puede administrar RabbitMQ.

## Alcance
**Incluye**
- `SecurityConfig` OAuth2 Resource Server en `mqadmin` (reutilizando `AudienceValidator` y `TenantIssuerValidator` del BFF).
- `@PreAuthorize("hasRole('Admin')")` en los endpoints de administración (o regla por ruta).
- `BffMqAdminController` (`/api/mqadmin/**`) que propaga el Bearer.
- Registro de `mqadmin` en `infra/apps/compose.yml` + variables de entorno.

**NO incluye**
- Frontend (tarea 12).

## Prerrequisitos
- Tarea 10 completada.
- Valores de Azure IDaaS conocidos (issuer/audience, ya en `bff/application.properties`).

## Pasos
1. Añadir a `mqadmin/pom.xml` (si no está) `spring-boot-starter-oauth2-resource-server`.
2. Crear `config/SecurityConfig.java` en mqadmin:
   - `.anyRequest().authenticated()` para `/api/mqadmin/**`.
   - `/actuator/health` permitido.
   - `JwtDecoder` con delegación de validadores (issuer v1/v2 + audience con/sin `api://`), reutilizando las clases del BFF (copiar a un paquete `config`).
   - Mapeo de roles `roles` → `ROLE_*` (copiar `JwtRoleConverter`).
3. Añadir `@EnableMethodSecurity` y `@PreAuthorize("hasRole('Admin')")` en `MqAdminController`.
4. En el BFF, crear `controller/BffMqAdminController`:
   - `@RequestMapping("/api/mqadmin")`, `WebClient` base `${services.mqadmin.url}`.
   - Propaga `Authorization: Bearer <jwt>` en cada operación, igual que los demás BFF controllers.
5. `bff/application.properties`: agregar `services.mqadmin.url=${SERVICES_MQADMIN_URL:http://localhost:8086}`.
6. `infra/apps/compose.yml`: agregar el servicio `mqadmin-svc` (puerto 8086, env RabbitMQ y Azure) y `SERVICES_MQADMIN_URL` en `bff-svc`.
7. Verificar 401 sin token y 200/201 con token de Admin.

## Archivos a crear / modificar
| Acción | Archivo |
|--------|---------|
| Modificar | `.../ms-andesstay-mqadmin/pom.xml` |
| Crear | `.../mqadmin/config/SecurityConfig.java`, `AudienceValidator.java`, `TenantIssuerValidator.java`, `JwtRoleConverter.java` |
| Modificar | `.../mqadmin/controller/MqAdminController.java` (`@PreAuthorize`) |
| Crear | `.../ms-andesstay-bff/controller/BffMqAdminController.java` |
| Modificar | `.../ms-andesstay-bff/src/main/resources/application.properties` |
| Modificar | `infra/apps/compose.yml` |

## Criterios de aceptación (Definition of Done)
- [ ] Sin token JWT → **401** en `/api/mqadmin/**`.
- [ ] Con token sin rol Admin → **403**.
- [ ] Con token de Admin → las operaciones funcionan vía BFF (`/api/mqadmin/...`).
- [ ] `mqadmin` aparece en el compose con su puerto y variables.
- [ ] Todos los módulos afectados compilan.

## Cómo verificar
```bash
# Sin token
curl -i http://localhost:8080/api/mqadmin/queues        # 401
# Con token (usar el frontend o un token de Admin válido)
curl -i http://localhost:8080/api/mqadmin/queues -H "Authorization: Bearer <jwt>"   # 200
```

## Rollback
```bash
git revert <hash>
```
> Si la seguridad rompe la operación, revertir deja el admin ms abierto pero funcional (no recomendado para la entrega; solo contingencia).

## Notas
- La pauta exige "filtros que validen el JWT" en el backend: esto cubre ese requisito para el nuevo servicio.
- Reutilizar los validadores del BFF evita duplicar la lógica de issuer v1/v2 y `api://`.
