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

---

## Resultado — 2026-10-16

### Ajustes respecto al plan original de este archivo

El snippet de `application.yml` de la sección *Pasos* quedó desactualizado frente a las decisiones posteriores (ver `_estado-ep3.md` §2). Se aplicó lo siguiente:

| Punto del archivo | Aplicado en la práctica | Motivo |
|-------------------|-------------------------|--------|
| `username: guest` / sin `virtual-host` | `andesstay` / `AndesStay2024!` / vhost `andesstay` | El broker de `infra/mq/compose.yml` **no** tiene usuario `guest` |
| Bloque `andesstay.messaging` solo con `exchange/dlx/dlq` | Bloque **completo** (con `dlq-routing-key`, `queues` y `routing-keys`), idéntico a `notify` | Una única convención de topología (indicador 1) |
| `spring-boot-starter-oauth2-resource-server` "puede añadirse luego" | **No** se añade en esta tarea | Trae `spring-boot-starter-security`, que dejaría `/actuator/health` en 401. Se añade en la tarea 11 con su `SecurityConfig` |
| (no estaba en la lista) | Se creó `config/MessagingProperties` | Enlazar el bloque `andesstay.messaging.*` (indicador 1); es configuración pura, sin lógica de administración |

### Archivos creados

```
Backend-api/ms-andesstay-mqadmin/
├── pom.xml                     parent Spring Boot 3.3.4, Java 21
├── Dockerfile                  multi-stage, EXPOSE 8086
├── mvnw / mvnw.cmd / .mvn/     wrapper copiado de notify
├── .gitignore / .gitattributes copiados de notify
└── src/main/
    ├── java/com/duocuc/ms_andesstay_mqadmin/
    │   ├── MsAndesstayMqadminApplication.java
    │   └── config/MessagingProperties.java
    └── resources/application.yml   puerto 8086 + rabbitmq + andesstay.messaging.*
```

Dependencias: `web`, `amqp`, `validation`, `actuator`, `test`. Se modificó `README.md` (mención del servicio en el puerto 8086). `git add -A --dry-run` confirma que `target/` y `maven-wrapper.jar` quedan excluidos por el `.gitignore` heredado.

### Verificación

**1. Compilación del módulo nuevo:** `.\mvnw.cmd -B clean package -DskipTests` → **BUILD SUCCESS**, jar `ms-andesstay-mqadmin-0.0.1-SNAPSHOT.jar`.

**2. Arranque real:** `java -jar target\...jar`
```text
Tomcat started on port 8086 (http) with context path '/'
Started MsAndesstayMqadminApplication in 2.019 seconds
```
`GET /actuator/health` → `HTTP 503` con `{"status":"DOWN"}`, y en el log:
```text
WARN o.s.b.a.amqp.RabbitHealthIndicator : Rabbit health check failed
org.springframework.amqp.AmqpConnectException: java.net.ConnectException: Connection refused
```
El servicio arranca correctamente; el `DOWN` es **solo** porque no hay broker en este entorno (ver hallazgo).

### Hallazgo de entorno (limita el DoD "health UP con RabbitMQ")

- **No hay broker disponible en esta máquina:** `localhost:5672` y `15672` cerrados, y no hay Docker/WSL instalados (no se pudo hacer `docker compose -f infra/mq/compose.yml up -d`). Por eso no se pudo verificar `/actuator/health` en `UP` con RabbitMQ conectado; sí se verificó el arranque y que el indicador `rabbit` reporta `DOWN` por ausencia de broker. La verificación completa requiere levantar `infra/mq` (como en las tareas 03–07).

- **`bff` y `reservations` no compilan en este entorno, de forma preexistente:** el `java` por defecto es **JDK 26.0.1**, y Lombok (el gestionado por Spring Boot 3.3.4) falla con JDK 26 →
  ```text
  Fatal error compiling: java.lang.ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN
  ```
  Afecta **solo** a los dos módulos que usan Lombok. Es independiente de esta tarea: `bff`/`reservations` están idénticos a `HEAD` (esta tarea solo añade un módulo standalone y 1 línea en `README.md`; no hay pom agregador en `Backend-api/`). Los otros 5 módulos (`audit`, `catalog`, `notify`, `report`, `mqadmin`) compilan con **BUILD SUCCESS**.

### Rollback

```bash
git revert <hash>
# o eliminar Backend-api/ms-andesstay-mqadmin/ por completo
```
Módulo aislado: no afecta a ningún servicio existente.
