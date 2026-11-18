# Fase 0 — Preparación

> **Estado:** completada (2026-11-17) · **Indicadores:** — · **Depende de:** — · **Est.:** 0,5 d

## Objetivo

Dejar el repositorio listo para la EP5: rama, plan, índice de tareas y baseline compilable.

## Pasos

1. Verificar `git status` limpio en `main` y `git pull`.
2. Crear la rama `feature/ep5-kafka`.
3. Crear `docs/PLAN_EP5_KAFKA.md`.
4. Crear `tareas/ep5/README.md` y los archivos de fase 00–08.
5. Compilar el baseline de los 7 microservicios y el frontend (sin cambios funcionales).

## Definition of Done

- [x] Rama `feature/ep5-kafka` creada.
- [x] Plan y tareas escritos y enlazados.
- [ ] Baseline compilado sin errores.
- [x] Decisiones de diseño registradas en el plan.

## Verificación

```powershell
git status -sb
./mvnw.cmd -q -DskipTests package   # en cada Backend-api/ms-*
npm run build                        # en Frontend/
```

## Commit sugerido

```text
docs(repo): agrega plan y tareas de la EP5 Kafka
```

## Rollback

```powershell
git checkout main
git branch -D feature/ep5-kafka
```
