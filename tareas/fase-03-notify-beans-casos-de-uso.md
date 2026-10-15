# Tarea 03 — notify: beans de configuración por caso de uso

- **Fase / Nº:** 03
- **Indicador(es) de la pauta:** **Indicador 2 (13%)** — beans `Queue`/`Exchange`/`Binding` por caso de uso
- **Depende de:** 02
- **Rama:** `feature/ep3-rabbitmq`
- **Commit sugerido:** `feat(notify): declarar beans Queue/Exchange/Binding por caso de uso`
- **Riesgo de rollback:** bajo

## Objetivo
Declarar, de forma explícita y comentada, los beans de topología para **cada** caso de uso del sistema (notificaciones, tickets de housekeeping, generación de documentos) y la DLQ, tal como exige la pauta.

## Alcance
**Incluye**
- Beans `Queue`, `Exchange`, `Binding` y DLX/DLQ parametrizados.
- Agrupación de beans por dominio funcional (métodos/prefijos claros).
- `MessageConverter` JSON y `RabbitTemplate`/`AmqpTemplate` con el converter.

**NO incluye**
- ACK manual ni container factory (tarea 05).
- Publishers/listeners (tareas 04 y 06).

## Prerrequisitos
- Tarea 02 completada (`MessagingProperties` disponible).

## Pasos
1. En `config/RabbitMQConfig.java` (o dividir en `config/messaging/*Config.java` por dominio) declarar:
   - **Exchange principal** topic/direct `notificationsExchange()` desde `properties.getExchange()`.
   - **DLX** `deadLetterExchange()` y **DLQ** `deadLetterQueue()` + su binding con `dlq-routing-key`.
   - Colas durables con argumentos de dead-lettering:
     ```
     x-dead-letter-exchange = properties.getDlx()
     x-dead-letter-routing-key = properties.getDlqRoutingKey()
     ```
   - Colas: `q.notify.email`, `q.notify.sms`, `q.notify.push`, `q.housekeeping.tickets`, `q.docs.generate`.
   - Un `Binding` por cola con su routing key (desde `properties.getRoutingKeys()`).
2. Documentar cada binding en el código (comentario: dominio → cola → routing key).
3. Bean `Jackson2JsonMessageConverter` + `RabbitTemplate` con el converter.
4. (Opcional, recomendado) Bean `RabbitAdmin` para asegurar la declaración al arrancar.
5. Evitar colisiones de bean por nombre; usar nombres de método únicos.

## Archivos a crear / modificar
| Acción | Archivo |
|--------|---------|
| Modificar | `.../notify/config/RabbitMQConfig.java` |
| Crear (opcional) | `.../notify/config/messaging/NotificationsTopologyConfig.java`, `HousekeepingTopologyConfig.java`, `DocumentsTopologyConfig.java` |

## Criterios de aceptación (Definition of Done)
- [x] Existe al menos un `Queue`, `Exchange` y `Binding` por cada caso de uso definido.
- [x] Cada ruta de mensajería está **claramente identificada y comentada** en código.
- [x] Todas las colas tienen dead-lettering configurado hacia DLX/DLQ.
- [x] No hay literales de nombres (todo desde `MessagingProperties`).
- [x] `notify` arranca y las 5 colas + DLQ aparecen en la UI de RabbitMQ.

## Cómo verificar
```bash
cd Backend-api/ms-andesstay-notify && .\mvnw.cmd spring-boot:run
```
- UI RabbitMQ → Queues: deben existir `q.notify.email`, `q.notify.sms`, `q.notify.push`, `q.housekeeping.tickets`, `q.docs.generate`, `q.andesstay.dlq`.
- Exchanges: `ex.andesstay.notifications` y `ex.andesstay.dlx`.
- Cada cola → pestaña *Bindings* y *Arguments* (`x-dead-letter-exchange`).

## Rollback
```bash
git revert <hash>
```
> Al revertir, las colas nuevas quedan huérfanas en el broker local; borrarlas manualmente o recrear el contenedor con volumen limpio.

## Notas
- Si se opta por separar en varias clases `@Configuration`, mantener el prefijo de nombre por dominio para facilitar la lectura de la pauta.

---

## Resultado — 2026-10-15

### Opción aplicada: **dividida por dominio** (aprobada por el equipo)

| Archivo | Contenido |
|---------|-----------|
| `config/messaging/MessagingInfraConfig.java` | `MessageConverter` JSON, `AmqpTemplate` y `RabbitAdmin` |
| `config/messaging/DeadLetterConfig.java` | `deadLetterExchange()`, `deadLetterQueue()`, `deadLetterBinding()` |
| `config/messaging/NotificationsTopologyConfig.java` | exchange principal + 3 colas y 3 bindings (email/sms/push) |
| `config/messaging/HousekeepingTopologyConfig.java` | cola y binding de tickets de aseo |
| `config/messaging/DocumentsTopologyConfig.java` | cola y binding de generación de documentos |
| `config/messaging/TopologySupport.java` | helper compartido que aplica el dead-lettering común |
| **Eliminado** `config/RabbitMQConfig.java` | reemplazado por los anteriores |

### Beans por caso de uso

| Caso de uso | `Queue` | `Exchange` | `Binding` (routing key) | Productor | Consumidor |
|-------------|---------|------------|-------------------------|-----------|------------|
| Notificación email | `q.notify.email` | `ex.andesstay.notifications` | `notify.email` | notify / reservations | notify |
| Notificación SMS | `q.notify.sms` | `ex.andesstay.notifications` | `notify.sms` | notify | notify |
| Notificación push | `q.notify.push` | `ex.andesstay.notifications` | `notify.push` | notify | notify |
| Ticket de housekeeping | `q.housekeeping.tickets` | `ex.andesstay.notifications` | `housekeeping.ticket` | reservations (tarea 07) | notify (tarea 06) |
| Documentos (voucher/factura) | `q.docs.generate` | `ex.andesstay.notifications` | `docs.generate` | reservations (tarea 07) | notify (tarea 06) |
| Descartes (transversal) | `q.andesstay.dlq` | `ex.andesstay.dlx` | `dlq.routing.key` | (automático) | notify |

Cada clase declara sus rutas en el Javadoc (`dominio → cola → routing key`), como pide el DoD.

`TopologySupport.durableWithDeadLetter(...)` centraliza los argumentos `x-dead-letter-exchange` / `x-dead-letter-routing-key`, así ninguna de las 5 colas se declara sin dead-lettering y no se repite lógica.

### Verificación

**1. Compilación:** `.\mvnw.cmd -B package -DskipTests` → `BUILD SUCCESS`, 0 `[ERROR]`, 0 `[WARNING]`.

**2. Sin literales en código:** el grep sobre `src/main/java` con `'"q\.|"ex\.|notify\.(email|sms|push)|housekeeping\.ticket|docs\.generate|dlq\.routing\.key|andesstay\.rabbitmq'` sólo devuelve las **rutas documentadas en Javadoc** (exigidas por el DoD). Filtrando comentarios (`-notmatch '^(\*|/\*|//)'`) el resultado es **0 coincidencias**: todo nombre proviene de `MessagingProperties`.

**3. Arranque:** `Started MsAndesstayNotifyApplication in 7.997 seconds` (`8083`), sin errores.

**4. Topología declarada en el broker** (Management API, vhost `andesstay`):

```
COLAS (6)
  q.andesstay.dlq          durable  (sin dead-lettering: es la DLQ)
  q.notify.email           durable  x-dead-letter-exchange=ex.andesstay.dlx, x-dead-letter-routing-key=dlq.routing.key
  q.notify.sms             durable  idem
  q.notify.push            durable  idem
  q.housekeeping.tickets   durable  idem
  q.docs.generate          durable  idem

EXCHANGES (2)
  ex.andesstay.notifications  direct  durable
  ex.andesstay.dlx            direct  durable

BINDINGS (6)
  ex.andesstay.dlx --[dlq.routing.key]--> q.andesstay.dlq
  ex.andesstay.notifications --[notify.email]--> q.notify.email
  ex.andesstay.notifications --[notify.sms]--> q.notify.sms
  ex.andesstay.notifications --[notify.push]--> q.notify.push
  ex.andesstay.notifications --[housekeeping.ticket]--> q.housekeeping.tickets
  ex.andesstay.notifications --[docs.generate]--> q.docs.generate
```

**5. Sin regresión funcional:** re-ejecutada la prueba de la tarea 02 → POST por EMAIL/SMS/PUSH y los 3 listeners consumen correctamente.

### Nota de diseño
El `RabbitAdmin` se declara explícitamente en `MessagingInfraConfig` (paso 4, recomendado) con el nombre de bean `amqpAdmin`, el mismo que usaría Spring Boot: sustituye al auto-configurado sin cambiar la semántica y deja explícito quién declara la topología.

### Rollback
```bash
git revert <hash>
# las colas nuevas quedan huérfanas en el broker local: borrarlas o recrear el volumen
```

