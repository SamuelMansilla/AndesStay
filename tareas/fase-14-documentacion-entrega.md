# Tarea 14 — Documentación, .gitignore y entrega

- **Fase / Nº:** 14
- **Indicador(es) de la pauta:** todas (consolida evidencia)
- **Depende de:** 13
- **Rama:** `feature/ep3-rabbitmq`
- **Commit sugerido:** `docs: documentar topología RabbitMQ, API admin y guía de pruebas EP3`
- **Riesgo de rollback:** bajo

## Objetivo
Consolidar la documentación de la entrega y verificar los requisitos formales (compilación, `.gitignore`, enlaces de GitHub) antes de subir a AVA.

## Alcance
**Incluye**
- Actualizar `README.md` (nuevo microservicio, topología, cómo probar).
- Crear/actualizar `docs/MENSAJERIA_RABBITMQ.md` (colas, exchanges, bindings, DLQ, convenciones, diagrama).
- Verificar `.gitignore` (no subir `target/`, `node_modules/`, secretos).
- Checklist final de compilación y builds.
- Marcar todas las tareas de `tareas/README.md` como completadas.

**NO incluye**
- Nuevas funcionalidades.

## Prerrequisitos
- Tareas 00–13 completadas.

## Pasos
1. Crear `docs/MENSAJERIA_RABBITMQ.md` con:
   - Diagrama de topología (producers → exchanges → colas → consumidores → DLX/DLQ).
   - Tabla de colas/exchanges/bindings y sus routing keys.
   - Estrategia de ACK/NACK, reintentos y DLQ.
   - Endpoints del microservicio admin y ejemplos `curl`.
   - Guía de prueba de DLQ.
2. Actualizar `README.md`:
   - Añadir `ms-andesstay-mqadmin` (puerto 8086) a la lista de microservicios y al "Inicio rápido".
   - Enlazar `docs/MENSAJERIA_RABBITMQ.md` y `context.md`.
3. Revisar `.gitignore`:
   - Confirmar `target/`, `node_modules/`, `dist/`, `.idea/`, `.vscode/`.
   - Verificar que **no** se commitean credenciales (ver riesgo R2 en `context.md`): mover a variables de entorno o dejar constancia.
4. Compilar todo:
   ```bash
   Get-ChildItem Backend-api -Directory | ForEach-Object {
     if (Test-Path "$($_.FullName)\pom.xml") {
       Push-Location $_.FullName; .\mvnw.cmd -q clean package; Pop-Location
     }
   }
   cd Frontend && npm run build
   ```
5. Prueba end-to-end de humo:
   - Levantar RabbitMQ + `notify` + `reservations` + `mqadmin` + `bff`.
   - Crear reserva → verificar `q.docs.generate`.
   - CHECKOUT → verificar `q.housekeeping.tickets`.
   - Enviar payload inválido → verificar `q.andesstay.dlq` + log.
   - Crear/eliminar cola vía UI del frontend.
6. Commit, merge a `main` y obtener los enlaces de GitHub para AVA.

## Archivos a crear / modificar
| Acción | Archivo |
|--------|---------|
| Crear | `docs/MENSAJERIA_RABBITMQ.md` |
| Modificar | `README.md` |
| Modificar | `.gitignore` (si aplica) |
| Modificar | `tareas/README.md` (checklist final) |

## Criterios de aceptación (Definition of Done)
- [ ] La documentación describe toda la topología y la estrategia de error.
- [ ] `README.md` menciona el nuevo microservicio y cómo probarlo.
- [ ] Todos los microservicios compilan y el frontend buildea.
- [ ] La prueba de humo end-to-end pasa (incluida la DLQ).
- [ ] `.gitignore` correcto; sin secretos en el repo.
- [ ] Enlaces de GitHub listos para AVA.

## Cómo verificar
```bash
git status                 # solo archivos esperados
git log --oneline -n 20    # un commit por tarea
```

## Rollback
```bash
git revert <hash>
```
> Solo documentación; sin impacto funcional.

## Notas
- Este commit cierra la EP3. Asegurar que `context.md` y `tareas/` reflejan el estado final.
- Si el docente pide cambios, abrir una tarea nueva siguiendo `_plantilla-tarea.md`.

---

## Resultado — 2026-10-16

### Documentación

- **Creado `docs/MENSAJERIA_RABBITMQ.md`**: diagrama de topología (productores → exchanges → colas → consumidores → DLX/DLQ), convenciones de nombres, tablas de exchanges/colas/bindings, contrato ACK/NACK/reintentos/DLQ, productores y consumidores, API de administración con ejemplos `curl` y códigos de error, guía de prueba end-to-end (incluida la DLQ), variables de entorno y mapa de "dónde vive la topología" en el código.
- **`README.md`**: enlace a `docs/MENSAJERIA_RABBITMQ.md` y a `context.md`; nuevo apartado de Inicio Rápido "Levantar RabbitMQ y los servicios de mensajería" (broker + `notify` 8083 + `mqadmin` 8086); el microservicio `mqadmin` ya estaba listado en la arquitectura.

### Verificación de requisitos formales

- **`.gitignore`** (raíz): cubre `target/`, `**/target/`, `node_modules/`, `dist/`, `.idea/`, `.vscode/`, logs. Confirmado que `target/` y `node_modules/` no se versionan.
- **Compilación final (JDK 21):** 7/7 módulos `BUILD SUCCESS` (`clean package -DskipTests`) y `Frontend` `vite build` OK.
- **Secretos (riesgo R2):** las credenciales de RabbitMQ y los identificadores de Azure siguen con *fallback* en `application.yml` versionados. Se dejó **constancia explícita** en `docs/MENSAJERIA_RABBITMQ.md` (sección de variables de entorno); la externalización/rotación queda pendiente de decisión del equipo.

### Limitación: prueba de humo end-to-end

No se pudo ejecutar la prueba de humo (levantar RabbitMQ + `notify` + `reservations` + `mqadmin` + `bff`) porque **este entorno no tiene Docker ni un broker RabbitMQ**. La secuencia exacta, con comandos, queda documentada en `docs/MENSAJERIA_RABBITMQ.md` → *Guía de prueba end-to-end*. Las partes verificables sin broker quedaron cubiertas por las pruebas automatizadas de la tarea 13 (contrato ACK/DLQ, `RabbitAdminService`, validación de la API).

### Pendiente de entrega

- `git push origin feature/ep3-rabbitmq` y **merge a `main`** (decisión del equipo).
- `git status` limpio y enlaces de GitHub para AVA.

### Rollback

```bash
git revert <hash>
```
Solo documentación; sin impacto funcional.
