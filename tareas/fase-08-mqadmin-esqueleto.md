# Tarea 08 — mqadmin: esqueleto del microservicio

- **Fase / Nº:** 08
- **Indicador(es) de la pauta:** — (habilita indicadores 6, 7 y 8)
- **Depende de:** 01
- **Rama:** `feature/ep3-rabbitmq`
- **Commit sugerido:** `feat(mqadmin): crear esqueleto del microservicio administrador de RabbitMQ`
- **Riesgo de rollback:** bajo

## Objetivo
Crear la base del microservicio dedicado **`ms-andesstay-mqadmin`** (puerto **8086**): proyecto Maven, aplicación Spring Boot, configuración de conexión a RabbitMQ y Dockerfile. Sin lógica de administración todavía (tarea 09).

## Alcance
**Incluye**
- Proyecto Maven con `pom.xml` (parent Spring Boot unificado en tarea 01).
- Clase `MsAndesstayMqadminApplication`.
- `application.yml` con `server.port=8086`, conexión RabbitMQ y `andesstay.messaging.*`.
- `Dockerfile` multi-stage (Java 21) y `.gitignore` heredado.
- Registro inicial en `README.md`.

**NO incluye**
- `RabbitAdminService` (tarea 09).
- Endpoints REST (tarea 10).
- Seguridad (tarea 11).

## Prerrequisitos
- Tarea 01 completada.
- RabbitMQ local disponible (tarea 00).

## Pasos
1. Crear la estructura:
   ```
   Backend-api/ms-andesstay-mqadmin/
   ├── pom.xml
   ├── Dockerfile
   └── src/main/java/com/duocuc/ms_andesstay_mqadmin/MsAndesstayMqadminApplication.java
   └── src/main/resources/application.yml
   ```
2. `pom.xml`: usar el **mismo parent** que el resto y dependencias:
   - `spring-boot-starter-web`
   - `spring-boot-starter-amqp`
   - `spring-boot-starter-validation`
   - `spring-boot-starter-actuator`
   - `spring-boot-starter-test` (test)
   - `spring-boot-starter-oauth2-resource-server` (se usará en tarea 11; puede añadirse luego)
3. `application.yml`:
   ```yaml
   server:
     port: 8086
   spring:
     application.name: ms-andesstay-mqadmin
     rabbitmq:
       host: ${RABBITMQ_HOST:localhost}
       port: ${RABBITMQ_PORT:5672}
       username: ${RABBITMQ_USER:guest}
       password: ${RABBITMQ_PASS:guest}
   andesstay:
     messaging:
       exchange: ex.andesstay.notifications
       dlx: ex.andesstay.dlx
       dlq: q.andesstay.dlq
   management.endpoints.web.exposure.include: health,info
   ```
4. `Dockerfile` multi-stage (copia de `notify/Dockerfile`) con `EXPOSE 8086`.
5. Verificar arranque (escuchando en 8086) y `/actuator/health`.

## Archivos a crear / modificar
| Acción | Archivo |
|--------|---------|
| Crear | `Backend-api/ms-andesstay-mqadmin/pom.xml` |
| Crear | `Backend-api/ms-andesstay-mqadmin/Dockerfile` |
| Crear | `.../mqadmin/MsAndesstayMqadminApplication.java` |
| Crear | `.../mqadmin/src/main/resources/application.yml` |
| Modificar | `README.md` (mención al nuevo servicio) |

## Criterios de aceptación (Definition of Done)
- [ ] El microservicio compila (`mvnw.cmd clean package`) y arranca en el puerto 8086.
- [ ] `/actuator/health` responde `UP` (RabbitMQ conectado).
- [ ] `Dockerfile` presente y coherente con los demás servicios.
- [ ] No se rompe el build del resto del monorepo.

## Cómo verificar
```bash
cd Backend-api/ms-andesstay-mqadmin
.\mvnw.cmd -q clean package -DskipTests
.\mvnw.cmd spring-boot:run
curl http://localhost:8086/actuator/health
```

## Rollback
```bash
git revert <hash>
# o eliminar la carpeta completa del microservicio
```
> Es un añadido aislado: no afecta a ningún servicio existente.

## Notas
- Elegir versión de Spring Boot idéntica a la del resto (coherencia).
- Mantener el mismo estilo de `Dockerfile` para facilitar el compose.
