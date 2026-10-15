# Tarea 00 — Preparación del entorno y rama de trabajo

- **Fase / Nº:** 00
- **Indicador(es) de la pauta:** — (habilitador)
- **Depende de:** ninguna
- **Rama:** `feature/ep3-rabbitmq`
- **Commit sugerido:** `chore(repo): crear rama y contexto de trabajo EP3 RabbitMQ`
- **Riesgo de rollback:** bajo

## Objetivo
Dejar el entorno listo (rama, broker local, baseline compilando) para empezar la integración de RabbitMQ sin sorpresas.

## Alcance
**Incluye**
- Confirmar baseline: todos los microservicios compilan y el frontend buildea.
- Levantar RabbitMQ local desde `infra/mq/compose.yml`.
- Crear la rama integradora.

**NO incluye**
- Cambios de código funcionales (van en tareas 02+).
- Unificar versiones de Spring Boot (tarea 01).

## Prerrequisitos
- Docker Desktop funcionando.
- JDK 21 y Maven wrapper (`mvnw.cmd`) disponibles.
- Node + npm para el frontend.

## Pasos
1. Crear la rama desde `main`:
   ```bash
   git checkout main && git pull
   git checkout -b feature/ep3-rabbitmq
   ```
2. Levantar RabbitMQ:
   ```bash
   docker compose -f infra/mq/compose.yml up -d
   ```
   Verificar UI en http://localhost:15672 (usuario `andesstay` / `AndesStay2024!`) y vhost `andesstay`.
3. Compilar baseline de cada microservicio (para detectar fallos previos):
   ```bash
   cd Backend-api/ms-andesstay-notify && .\mvnw.cmd -q clean package -DskipTests
   cd ../ms-andesstay-reservations && .\mvnw.cmd -q clean package -DskipTests
   cd ../ms-andesstay-mqadmin   # aún no existe (tarea 08)
   ```
4. Build del frontend baseline:
   ```bash
   cd Frontend && npm install && npm run build
   ```
5. Anotar en esta tarea cualquier error de baseline encontrado (no arreglarlo aquí; se aborda en tarea 01 o en una tarea propia).

## Archivos a crear / modificar
| Acción | Archivo |
|--------|---------|
| — | (sin cambios de código; opcional: notas en `tareas/README.md`) |

## Criterios de aceptación (Definition of Done)
- [x] Rama `feature/ep3-rabbitmq` creada y activa.
- [x] RabbitMQ local en ejecución (management UI accesible).
- [x] Baseline documentado: qué compila y qué no.
- [x] `README.md` de `tareas/` marcado con la tarea 00 como hecha.

## Cómo verificar
```bash
git branch --show-current        # feature/ep3-rabbitmq
docker ps --filter name=rabbitmq # rabbitmq-1 y rabbitmq-2 arriba
```
- Abrir http://localhost:15672 y confirmar vhost `andesstay`.

## Rollback
```bash
git checkout main
docker compose -f infra/mq/compose.yml down
```

## Notas
- Si RabbitMQ local no arranca, revisar el archivo `infra/mq/rabbitmq.conf` referenciado por el compose (debe existir).
- Guardar la salida del baseline de compilación para comparar al final.

---

## Resultado del baseline — 2026-10-15

**Entorno:** Windows · Docker 29.1.2 · JDK 21.0.9 · Node 25.2.1 / npm 11.6.2 · rama `feature/ep3-rabbitmq`.

### Compilación (baseline verde, 6/6)

| Módulo | Comando | Resultado | Artefacto |
|--------|---------|-----------|-----------|
| `ms-andesstay-audit` | `mvnw.cmd -B clean package -DskipTests` | ✅ BUILD SUCCESS (0 errores / 0 warnings) | `ms-andesstay-audit-0.0.1-SNAPSHOT.jar` (77.4 MB) |
| `ms-andesstay-bff` | idem | ✅ BUILD SUCCESS | `ms-andesstay-bff-0.0.1-SNAPSHOT.jar` (47.5 MB) |
| `ms-andesstay-catalog` | idem | ✅ BUILD SUCCESS | `ms-andesstay-catalog-0.0.1-SNAPSHOT.jar` (48.9 MB) |
| `ms-andesstay-notify` | idem | ✅ BUILD SUCCESS | `ms-andesstay-notify-0.0.1-SNAPSHOT.jar` (21.5 MB) |
| `ms-andesstay-report` | idem | ✅ BUILD SUCCESS | `ms-andesstay-report-0.0.1-SNAPSHOT.jar` (62.6 MB) |
| `ms-andesstay-reservations` | idem | ✅ BUILD SUCCESS | `ms-andesstay-reservations-0.0.1-SNAPSHOT.jar` (74.6 MB) |
| `Frontend` | `npm install` + `npm run build` | ✅ OK (exit 0) | `Frontend/dist/index.html` generado |

> Los tests se omitieron (`-DskipTests`) en el baseline; su ejecución corresponde a la tarea 13.

### RabbitMQ local

- `docker compose -f infra/mq/compose.yml up -d` → `rabbitmq-1` y `rabbitmq-2` **Up**.
- Puertos: AMQP `5672`/`5673` · Management UI `15672`/`15673`.
- Verificado por Management API: **RabbitMQ 3.13.7**, cluster `rabbit@rabbitmq-1`, vhost **`andesstay`** existente, login `andesstay` OK.

### Bloqueadores encontrados y resueltos en esta tarea

1. **Docker daemon no estaba corriendo** → se levantó Docker Desktop antes de ejecutar el compose.
2. **`infra/mq/rabbitmq.conf` no existía** aunque `compose.yml` lo monta en `/etc/rabbitmq/rabbitmq.conf` (Docker habría creado un directorio y RabbitMQ habría fallado al arrancar) → se creó el archivo mínimo (listeners 5672, management 15672, watermark de memoria/disco, logging a consola). Confirmado cargado sin errores en ambos nodos.

### Hallazgos pre-existentes (NO se corrigen aquí; van en tarea 01 o propia)

1. **Versiones Spring Boot inconsistentes (riesgo R3 del contexto):**

   | Módulo | Spring Boot (parent) | `java.version` |
   |--------|----------------------|----------------|
   | `ms-andesstay-audit` | 4.1.1 | 17 |
   | `ms-andesstay-catalog` | 4.1.1 | 17 |
   | `ms-andesstay-bff` | 4.0.8 | 21 |
   | `ms-andesstay-reservations` | 4.0.8 | 21 |
   | `ms-andesstay-notify` | 3.3.4 | 21 |
   | `ms-andesstay-report` | 3.3.4 | 21 |

   Compilan igualmente porque cada módulo es un proyecto Maven independiente (no hay reactor padre común), pero conviene unificar en la tarea 01. Nota: `spring-boot-starter-amqp` está en la línea 3.x de `notify`, así que unificar a 4.x implicará revisar esa dependencia.

2. **Desalineación de credenciales/vhost de RabbitMQ:** `infra/mq/compose.yml` usa usuario `andesstay`, password `AndesStay2024!` y vhost `andesstay`, mientras que `ms-andesstay-notify/src/main/resources/application.properties` apunta a `localhost:5672` con `guest`/`guest` y vhost por defecto `/`. Lo local se levantó con las credenciales del compose; la configuración del microservicio se aborda en la tarea 02.
3. **Secretos en archivos versionados (riesgo R2):** credenciales de Supabase y de RabbitMQ presentes en propiedades/compose. Pendiente de decisión (§10, punto 3).

### Rollback de esta tarea

```bash
git checkout main
docker compose -f infra/mq/compose.yml down
```

