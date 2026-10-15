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
