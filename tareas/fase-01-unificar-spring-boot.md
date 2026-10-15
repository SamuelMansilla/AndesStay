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
- [ ] Todos los `pom.xml` heredan la **misma** versión de Spring Boot.
- [ ] Todos los microservicios compilan sin errores.
- [ ] `notify` y `reservations` arrancan correctamente.
- [ ] No hay cambios funcionales en el diff.

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
