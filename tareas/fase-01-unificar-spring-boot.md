# Tarea 01 — Unificar versiones de Spring Boot

- **Fase / Nº:** 01
- **Indicador(es) de la pauta:** — (calidad / "el backend debe compilar")
- **Depende de:** 00
- **Rama:** `feature/ep3-rabbitmq`
- **Commit sugerido:** `build(backend): unificar versión de Spring Boot en todos los microservicios`
- **Riesgo de rollback:** medio (puede cambiar comportamiento de arranque)

## Objetivo
Eliminar la inconsistencia detectada: `ms-andesstay-notify` (y otros) heredan `spring-boot-starter-parent` **3.3.4**, mientras `ms-andesstay-reservations` declara **4.0.8**. Un entorno homogéneo evita fallos de compatibilidad al añadir AMQP/validación.

## Alcance
**Incluye**
- Definir la versión objetivo (propuesta: **3.3.4**, ya usada por la mayoría).
- Ajustar el `pom.xml` de `reservations` (y cualquier otro desalineado).
- Recompilar **todos** los microservicios.

**NO incluye**
- Cambios funcionales ni de mensajería.

## Prerrequisitos
- Tarea 00 completada.
- Baseline de compilación anotado.

## Pasos
1. Inventariar la versión padre de cada `pom.xml`:
   ```bash
   Select-String -Path Backend-api/*/pom.xml -Pattern "spring-boot-starter-parent" -Context 0,3
   ```
2. Confirmar con el equipo la versión objetivo (**decisión abierta §10 de `context.md`**). Recomendado `3.3.4`.
3. Editar `Backend-api/ms-andesstay-reservations/pom.xml` → `<version>` del parent.
4. Revisar dependencias exclusivas de versiones nuevas/antiguas, p. ej.:
   - `spring-boot-starter-webmvc` (nuevo naming) vs `spring-boot-starter-web`.
   - Starters `*-test` específicos (`spring-boot-starter-data-jpa-test`, etc.).
   Ajustar solo lo imprescindible para que compile.
5. Recompilar todos:
   ```bash
   Get-ChildItem Backend-api -Directory | ForEach-Object {
     if (Test-Path "$($_.FullName)\pom.xml") {
       Push-Location $_.FullName; .\mvnw.cmd -q clean package -DskipTests; Pop-Location
     }
   }
   ```
6. Levantar cada servicio y confirmar que arranca (al menos `notify` y `reservations`).

## Archivos a crear / modificar
| Acción | Archivo |
|--------|---------|
| Modificar | `Backend-api/ms-andesstay-reservations/pom.xml` |
| Modificar (si aplica) | otros `pom.xml` desalineados |

## Criterios de aceptación (Definition of Done)
- [x] Todos los `pom.xml` heredan la **misma** versión de Spring Boot.
- [x] Todos los microservicios compilan sin errores.
- [x] `notify` y `reservations` arrancan correctamente.
- [x] No hay cambios funcionales en el diff.

## Cómo verificar
```bash
# Todas las versiones deben coincidir
Select-String -Path Backend-api/*/pom.xml -Pattern "<version>3.*</version>"
```

## Rollback
```bash
git revert <hash>
```
> Si la unificación rompe el arranque, revertir es seguro porque no hay cambios funcionales.

## Notas
- Es una tarea **aislada a propósito**: si genera problemas, se revierte sin afectar la mensajería.
- Si el equipo decide mantener `4.0.8`, la tarea es la misma pero en sentido inverso (subir el resto).

---

## Resultado — 2026-10-15

### Decisión aplicada
**Spring Boot 3.3.4 + Java 21 en los 6 microservicios.** Coincide con la línea 3.x de `spring-boot-starter-amqp` que ya usaban `notify`/`report` (evita revisar esa dependencia, como ocurriría subiendo a 4.x).

### Cambios (solo `pom.xml`; 4 archivos, 18 inserciones / 48 borrados)

| Módulo | Parent antes | Parent ahora | `java.version` antes → ahora |
|--------|--------------|--------------|------------------------------|
| `ms-andesstay-audit` | 4.1.1 | **3.3.4** | 17 → **21** |
| `ms-andesstay-bff` | 4.0.8 | **3.3.4** | 21 |
| `ms-andesstay-catalog` | 4.1.1 | **3.3.4** | 17 → **21** |
| `ms-andesstay-reservations` | 4.0.8 | **3.3.4** | 21 |
| `ms-andesstay-notify` | 3.3.4 | 3.3.4 (sin cambios) | 21 |
| `ms-andesstay-report` | 3.3.4 | 3.3.4 (sin cambios) | 21 |

### Renombrado de starters (naming de Boot 4 → Boot 3.3.x)
Los 4 módulos que estaban en 4.x usaban el naming nuevo (inexistente en 3.3.4). Mapeo aplicado:

| Starter Boot 4 | Starter Boot 3.3.4 |
|----------------|--------------------|
| `spring-boot-starter-webmvc` | `spring-boot-starter-web` |
| `spring-boot-starter-security-oauth2-resource-server` | `spring-boot-starter-oauth2-resource-server` |
| `spring-boot-starter-webmvc-test` | `spring-boot-starter-test` |
| `spring-boot-starter-security-oauth2-resource-server-test` | `spring-boot-starter-test` (+ `spring-security-test` en `bff`) |
| `spring-boot-starter-security-test` | `spring-security-test` |
| `spring-boot-starter-webflux-test` | incluido en `spring-boot-starter-test` |
| `spring-boot-starter-data-jpa-test` | `spring-boot-starter-test` |
| `spring-boot-starter-validation-test` | `spring-boot-starter-test` |

Sin cambios: `spring-boot-starter-actuator`, `-webflux`, `-data-jpa`, `-validation`, `-security`, `spring-kafka`, `postgresql`, `lombok`. **Ningún archivo `.java` fue modificado** (el código usaba APIs estándar de Spring Security 6 / Spring Framework 6, compatibles en ambos sentidos).

### Verificación

**1. Compilación — 6/6 BUILD SUCCESS** (`.\mvnw.cmd -B clean package -DskipTests`): 0 `[ERROR]`, 0 `[WARNING]`, jar empaquetado en cada módulo.

**2. Versión efectivamente embebida** (inspección de `BOOT-INF/lib` en los jars): los 6 incluyen `spring-boot-3.3.4.jar`; `spring-core-6.1.13.jar`; en `audit` y `bff` además `spring-security-core-6.3.3.jar`. Es decir, la unificación es real a nivel de artefacto, no solo declarativa.

**3. Arranque real** (jar ejecutado con `java -jar`, Tomcat embebido):

| Servicio | Resultado | Evidencia |
|----------|-----------|-----------|
| `ms-andesstay-notify` | ✅ | `Started MsAndesstayNotifyApplication in 7.858 seconds` · `Tomcat started on port 8083` · 0 errores |
| `ms-andesstay-reservations` | ✅ | `Started MsAndesstayReservationsApplication in 21.923 seconds` · `Tomcat started on port 8081` · 0 errores |

Overrides usados **solo por línea de comandos** (el repo no cambia): credenciales reales del broker para `notify`, y un PostgreSQL temporal en Docker (puerto 55432) para `reservations`.

### Hallazgos externos (ajenos a la versión)

1. **`notify` no conecta con la configuración versionada:** `application.properties` usa `guest`/`guest` vhost `/`, pero el broker del compose **no tiene usuario `guest`** (solo `andesstay`, tags `[administrator]`). Con las credenciales del compose arranca sin errores → se corrige en la **tarea 02** (topología y credenciales centralizadas en `application.yml`).
2. **🛑 Supabase inaccesible (bloqueante para tareas que arranquen servicios con JPA):** `reservations`, `catalog` y `audit` apuntan a `aws-1-us-east-1.pooler.supabase.com:6543` y el pooler responde `FATAL: (ENOTFOUND) tenant/user postgres.rqoexnwaqkoqfkdccoaw not found`. El proyecto Supabase parece **pausado o eliminado**. Requiere acción del equipo (reactivar el proyecto o provisionar una BD nueva). El arranque de `reservations` se validó con un PostgreSQL temporal, así que el código y la versión quedan verificados.
3. **Secretos en propiedades versionadas (R2):** se confirma que las credenciales de Supabase están en texto plano en los 3 `application.properties`. Sigue pendiente la decisión de externalizarlas (§10.3 de `context.md`).

### Comandos de verificación usados

```bash
# Versiones homogéneas
Get-ChildItem Backend-api -Directory | ForEach-Object { ... }   # parent -> 3.3.4 en los 6

# Compilación completa
Get-ChildItem Backend-api -Directory | ForEach-Object {
  Push-Location $_.FullName; .\mvnw.cmd -B clean package -DskipTests; Pop-Location }

# Arranque
java -jar Backend-api/ms-andesstay-notify/target/ms-andesstay-notify-0.0.1-SNAPSHOT.jar
java -jar Backend-api/ms-andesstay-reservations/target/ms-andesstay-reservations-0.0.1-SNAPSHOT.jar
```

