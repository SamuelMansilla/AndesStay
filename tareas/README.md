# Plan de trabajo EP3 — RabbitMQ (AndesStay · DSY1107)

Carpeta de **tareas atómicas** para integrar RabbitMQ al sistema cumpliendo la pauta de la Evaluación Parcial N°3.

Cada archivo de esta carpeta está pensado para **terminarse y commitearse de forma independiente**, de modo que:
- el avance sea **modular** (una responsabilidad por tarea),
- se pueda **revertir** una tarea sin deshacer las anteriores (`git revert <hash>`),
- el diff de cada commit sea **revisable** y contenga sentido completo.

> Contexto funcional y de arquitectura: ver [`../context.md`](../context.md).

---

## Flujo de trabajo

1. Elegir la siguiente tarea **no bloqueada** (respeta la columna *Depende de*).
2. Crear la rama de la tarea (o trabajar sobre la rama integradora, ver más abajo).
3. Implementar siguiendo los **Pasos**, hasta cumplir el **Definition of Done**.
4. Correr los comandos de **Verificación**.
5. Commit único con el mensaje sugerido.
6. Actualizar el **checklist de progreso** de este README.
7. Si algo falla → **Rollback** (sección de la tarea).

## Estrategia de ramas y rollback

- **Rama integradora:** `feature/ep3-rabbitmq` (una sola rama, un commit por tarea).
  - Ventaja: historial lineal, rollback trivial con `git revert <hash>`.
- **Alternativa (por sub-ramas):** `feature/ep3-<tarea>` + PR a la rama integradora.
  - Rollback: cerrar/eliminar la rama o `git revert` del merge.
- **Nunca** un commit mezcla dos tareas. Si una tarea toca varios microservicios, se parte en tareas separadas (ver Fase 7).

### Convención de commits (Conventional Commits)

```
<tipo>(<ámbito>): <descripción corta en imperativo>

[cuerpo opcional: tarea, indicador de pauta que cubre]
```

Tipos: `feat`, `fix`, `refactor`, `chore`, `test`, `docs`, `build`.
Ámbitos: `notify`, `reservations`, `mqadmin`, `bff`, `frontend`, `infra`, `repo`.

Ejemplo: `feat(notify): centralizar topología RabbitMQ en MessagingProperties`

---

## Índice de tareas

| # | Tarea | Indicador pauta | Depende de | Est. | Estado |
|---|-------|-----------------|------------|------|--------|
| 00 | [Preparación del entorno](fase-00-preparacion.md) | — | — | 0.5 d | [x] |
| 01 | [Unificar versiones Spring Boot](fase-01-unificar-spring-boot.md) | — | 00 | 0.5 d | [x] |
| 02 | [notify: centralizar topología](fase-02-notify-centralizar-topologia.md) | 1 (12%) | 01 | 0.5 d | [x] |
| 03 | [notify: beans por caso de uso](fase-03-notify-beans-casos-de-uso.md) | 2 (13%) | 02 | 0.5 d | [x] |
| 04 | [notify: publishers y separación](fase-04-notify-publishers-separacion.md) | 3 (10%) | 03 | 0.5 d | [x] |
| 05 | [notify: ACK, reintentos y DLQ](fase-05-notify-ack-reintentos-dlq.md) | 5 (20%) | 03 | 1 d | [x] |
| 06 | [notify: consumidores por dominio](fase-06-notify-consumidores-por-dominio.md) | 4 (15%) | 04, 05 | 1 d | [ ] |
| 07 | [reservations: publicar tickets y documentos](fase-07-reservations-productores.md) | 1, 3 | 03 | 1 d | [ ] |
| 08 | [mqadmin: esqueleto del microservicio](fase-08-mqadmin-esqueleto.md) | — | 01 | 0.5 d | [ ] |
| 09 | [mqadmin: RabbitAdminService](fase-09-mqadmin-rabbitadmin-service.md) | 7 (10%) | 08 | 1 d | [ ] |
| 10 | [mqadmin: REST + validación](fase-10-mqadmin-rest-validacion.md) | 6 (13%), 8 (7%) | 09 | 1 d | [ ] |
| 11 | [mqadmin: seguridad, BFF e infra](fase-11-mqadmin-seguridad-bff-infra.md) | JWT / 6 | 10 | 0.5 d | [ ] |
| 12 | [frontend: panel de mensajería](fase-12-frontend-admin.md) | 6 (apoyo) | 11 | 1 d | [ ] |
| 13 | [Pruebas](fase-13-pruebas.md) | 5, 7, 8 | 06, 10 | 1 d | [ ] |
| 14 | [Documentación y entrega](fase-14-documentacion-entrega.md) | todas | 13 | 0.5 d | [ ] |

**Total estimado:** ~11–12 días de trabajo efectivo (holgado frente a las 2 semanas del encargo).

### Cobertura de la pauta

- Indicador 1 (12%) → Tareas 02, 07
- Indicador 2 (13%) → Tarea 03
- Indicador 3 (10%) → Tareas 04, 07
- Indicador 4 (15%) → Tarea 06
- Indicador 5 (20%) → Tareas 05, 13
- Indicador 6 (13%) → Tareas 10, 11, 12
- Indicador 7 (10%) → Tarea 09
- Indicador 8 (7%) → Tareas 10, 13

---

## Definición de "tarea terminada" (DoD global)

Una tarea se considera cerrada cuando:
- [ ] Cumple todos los criterios de aceptación de su archivo.
- [ ] Todos los módulos afectados **compilan** (`mvnw.cmd -q clean package`) y/o el frontend **buildea** (`npm run build`).
- [ ] No se introdujeron nombres de colas/exchanges/bindings **hardcodeados** en clases de negocio.
- [ ] El commit sigue la convención y menciona la tarea + indicador.

## Plantilla

Usar [`_plantilla-tarea.md`](_plantilla-tarea.md) para crear tareas nuevas.
