# Tarea 05 — notify: ACK manual, reintentos y DLQ

- **Fase / Nº:** 05
- **Indicador(es) de la pauta:** **Indicador 5 (20%)** — ACK/NACK y manejo explícito de errores
- **Depende de:** 03
- **Rama:** `feature/ep3-rabbitmq`
- **Commit sugerido:** `feat(notify): ACK manual, reintentos acotados y ruteo a DLQ`
- **Riesgo de rollback:** medio (cambia el modo de acknowledgement)

## Objetivo
Implementar confirmación explícita de mensajes y una estrategia clara de errores: diferencia entre recuperable/no recuperable, reintentos acotados y envío a DLQ, con **logging** de todo descarte. Es el indicador de mayor peso.

## Alcance
**Incluye**
- `SimpleRabbitListenerContainerFactory` con `AcknowledgeMode.MANUAL`.
- Uso de `Channel` + `deliveryTag` en listeners (`basicAck` / `basicNack`).
- `RetryInterceptor` con `MessageRecoverer` (republish a DLX al agotar intentos).
- `RabbitListenerErrorHandler` para errores no recuperables (`AmqpRejectAndDontRequeueException`).
- Config: `defaultRequeueRejected=false`, `missingQueuesFatal=false`.

**NO incluye**
- Reorganizar los listeners por dominio (tarea 06) — aquí se aplica el patrón de ACK.
- Pruebas automatizadas completas (tarea 13).

## Prerrequisitos
- Tarea 03 completada (DLX/DLQ declarados).

## Pasos
1. Crear `config/RabbitListenerConfig.java`:
   ```java
   @Bean
   public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
           ConnectionFactory cf, MessageConverter converter) {
       var factory = new SimpleRabbitListenerContainerFactory();
       factory.setConnectionFactory(cf);
       factory.setMessageConverter(converter);
       factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
       factory.setDefaultRequeueRejected(false);
       factory.setMissingQueuesFatal(false);
       factory.setConcurrentConsumers(2);
       factory.setPrefetchCount(10);
       return factory;
   }
   ```
2. Configurar reintentos con recuperación a DLQ (bean `RetryInterceptor` o `AdviceChain`):
   - `RetryTemplate` con `FixedBackOff` (p. ej. 3 intentos, 2s) y `RepublishMessageRecoverer(amqpTemplate, properties.getDlx(), properties.getDlqRoutingKey())`.
   - `RepublishMessageRecoverer` agrega headers de excepción (`x-exception-message`, `x-original-queue`).
3. Crear `messaging/MessageErrorHandler` (o usar `RabbitListenerErrorHandler`) que:
   - Registre en `ERROR` el fallo con `deliveryTag`, cola y payload.
   - Envuelva errores no recuperables para que no se reencolen.
4. Implementar en los listeners (o en un `AbstractDlqListener`) el contrato:
   ```java
   @RabbitListener(queues = "${andesstay.messaging.queues.notify-email}")
   public void onEmail(NotificationEvent ev, Channel ch,
                       @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                       @Header(AmqpHeaders.REDELIVERED) boolean redelivered) throws IOException {
       try {
           // lógica de negocio
           ch.basicAck(tag, false);
       } catch (RecoverableException e) {
           log.warn("Reintento...", e);
           ch.basicNack(tag, false, true);   // o dejar que el RetryTemplate republique
       } catch (NonRecoverableException e) {
           log.error("Descartando a DLQ...", e);
           ch.basicNack(tag, false, false);  // a DLQ
       }
   }
   ```
5. Definir excepciones de dominio: `RecoverableException` y `NonRecoverableException` (o usar `AmqpRejectAndDontRequeueException` para el no recuperable).
6. Verificar que un payload inválido termina en `q.andesstay.dlq` con log en ERROR.

## Archivos a crear / modificar
| Acción | Archivo |
|--------|---------|
| Crear | `.../notify/config/RabbitListenerConfig.java` |
| Crear | `.../notify/messaging/MessageErrorHandler.java` |
| Crear | `.../notify/messaging/exception/RecoverableException.java`, `NonRecoverableException.java` |
| Modificar | `.../notify/service/NotificationListener.java` (aplicar patrón) |
| Modificar | `application.yml` (`spring.rabbitmq.listener.simple.*`) |

## Criterios de aceptación (Definition of Done)
- [x] Los consumidores usan **ACK manual** (`AcknowledgeMode.MANUAL` + `basicAck`).
- [x] Existe distinción explícita entre error recuperable y no recuperable.
- [x] Los mensajes que fallan definitivamente llegan a `q.andesstay.dlq`.
- [x] Todo descarte queda registrado en **logs** con contexto (cola, deliveryTag, motivo).
- [x] No hay *requeue* infinito.

## Cómo verificar
```bash
# 1. Levantar notify y RabbitMQ
# 2. Forzar error: enviar payload que dispare NonRecoverableException
curl -X POST http://localhost:8083/api/notify/send -H "Content-Type: application/json" \
  -d '{"channel":"EMAIL","recipient":"","body":""}'
# 3. Ver la cola DLQ en la UI → debe incrementar
# 4. Revisar logs de notify → WARN/ERROR del descarte
```

## Rollback
```bash
git revert <hash>
```
> Al revertir se vuelve a ACK automático: seguro pero pierde el indicador. Revertir solo si bloquea el arranque.

## Notas
- Con ACK manual, si el consumidor cae **después** de procesar pero antes del `basicAck`, el mensaje se reentrega → la lógica debe ser **idempotente** (usar `notificationId`/`eventId`).
- Documentar en el código la decisión recuperable vs no recuperable para el evaluador.

---

## Resultado — 2026-10-15

### Diseño aplicado (decisión del equipo)

Reintentos **dentro del listener** y DLQ por *dead-lettering* de la propia cola. Se descartó el advice chain del contenedor (`RetryInterceptor` + `RepublishMessageRecoverer`) porque en `AcknowledgeMode.MANUAL` el `recoverer` republica el mensaje mientras el ACK sigue pendiente del listener: el original quedaría sin confirmar y se reentregaría al cerrarse el canal (duplicados). La decisión está documentada en el Javadoc de `AbstractDlqListener`.

### Contrato de confirmación (`AbstractDlqListener`)

| Situación | Acción AMQP | Destino | Log |
|-----------|-------------|---------|-----|
| `process(...)` termina bien | `basicAck(tag, false)` | confirmado y eliminado | `INFO [ACK]` |
| `RecoverableException` | reintentos acotados y al agotarlos `basicNack(tag, false, false)` | `q.andesstay.dlq` | `WARN [REINTENTO]` + `ERROR [DESCARTE->DLQ]` |
| `NonRecoverableException` u otro error inesperado | `basicNack(tag, false, false)` | `q.andesstay.dlq` | `ERROR [DESCARTE->DLQ]` |

`requeue=false` en **todos** los rechazos: el mensaje nunca vuelve a la cola de origen, se elimina la posibilidad de reentrega infinita y el ruteo final lo hace el `x-dead-letter-exchange` declarado en la tarea 03 (que además agrega los headers `x-death` con cola y motivo).

### Archivos

| Acción | Archivo | Rol |
|--------|---------|-----|
| Crear | `config/RabbitListenerConfig.java` | factory MANUAL + `RetryTemplate` (3 intentos / 2 s, sólo recuperables) |
| Crear | `messaging/AbstractDlqListener.java` | contrato de ACK/NACK + logs |
| Crear | `messaging/exception/RecoverableException.java` | error transitorio (se reintenta) |
| Crear | `messaging/exception/NonRecoverableException.java` | error definitivo (no se reintenta) |
| Crear | `config/NotificationSimulationProperties.java` | gancho de fallo transitorio, vacío por defecto |
| Modificar | `service/NotificationListener.java` | extiende el contrato + validación defensiva |
| Modificar | `model/NotificationEvent.java` | `toString()` para el contexto de los logs |
| Modificar | `application.yml` | gancho de simulación + nota de dónde se configura el listener |

### Detalles que sostienen el indicador

- **ACK explícito**: `channel.basicAck(deliveryTag, false)` después del procesamiento; cada confirmación queda en el log con canal, `deliveryTag` y si venía reentregado.
- **Distinción recuperable / no recuperable**: `RecoverableException` se reintenta; `NonRecoverableException` no. El `SimpleRetryPolicy` se declara con `defaultValue=false`, así que cualquier excepción no declarada tampoco se reintenta.
- **Validación defensiva en el consumidor**: `recipient`/`body` vacíos, `channel` del evento incoherente con la cola o payload nulo → `NonRecoverableException` → DLQ. Cubre el caso de un productor defectuoso que no pasa por la validación de la API.
- **Logs con contexto**: `[DESCARTE->DLQ] canal=... deliveryTag=... motivo=... excepcion=... mensaje=... evento=...` (con stacktrace).
- **Idempotencia**: `process(...)` sólo registra el envío simulado, sin efectos persistentes, por lo que es idempotente por construcción; queda anotado en el código que si se conecta un proveedor real habría que deduplicar por `notificationId`.

### Cambio de comportamiento documentado: se elimina el listener de la DLQ

`NotificationListener.processDeadLetter` (que consumía `${andesstay.messaging.dlq}`) fue **eliminado**. Si la DLQ tuviera consumidor, los mensajes descartados se consumirían y desaparecerían, con lo que dejaría de ser una cola de supervisión y se perdería la evidencia del indicador 5 (además, al no tener DLX propia, un fallo de ese consumidor no tendría salida). La DLQ queda como destino **terminal**, inspeccionable desde la UI o desde `mqadmin` (tareas 09–12); el descarte ya queda registrado en `ERROR` por el listener que lo rechaza, que es donde está el contexto.

### Configuración: por qué Java y no `application.yml`

El enunciado sugería `spring.rabbitmq.listener.simple.*`. Al declarar un `rabbitListenerContainerFactory` propio se sustituye el de Spring Boot y esas claves pasarían a ser **ignoradas** (configuración repartida y engañosa). Por eso el ACK manual, el requeue, `missingQueuesFatal`, el prefetch y los consumidores viven en `RabbitListenerConfig`, y el `application.yml` sólo deja la nota que apunta allí.

### Verificación (todo en una sola instancia)

Arranque: `Started MsAndesstayNotifyApplication in 10.163 seconds`, con `--andesstay.notify.simulation.fail-recipients=retry@andesstay.cl` para poder disparar el camino recuperable.

**A) Camino feliz → ACK explícito**
```text
INFO [EMAIL] Enviado a: feliz@andesstay.cl para la reserva #1001. Contenido: Camino feliz
INFO [ACK] canal=EMAIL deliveryTag=1 reentregado=false evento=NotificationEvent{notificationId='7d474d30-…', reservationId=1001, …}
```

**B) Error no recuperable** (payload inválido inyectado directo al exchange con la Management API, simulando un productor defectuoso que no pasa por la API de notify)
```text
ERROR [DESCARTE->DLQ] canal=EMAIL deliveryTag=1 motivo=error no recuperable (no se reintenta)
      excepcion=…NonRecoverableException mensaje=Payload sin destinatario (recipient) en EMAIL
      evento=NotificationEvent{notificationId='invalido-1', reservationId=1002, recipient='', …}
```
Sin ningún `[REINTENTO]` previo: los no recuperables no se reintentan.

**C) Error recuperable → 3 intentos y descarte**
```text
WARN  [REINTENTO] canal=EMAIL deliveryTag=2 intento=2 evento=…recipient='retry@andesstay.cl'…
WARN  [REINTENTO] canal=EMAIL deliveryTag=2 intento=3 evento=…recipient='retry@andesstay.cl'…
ERROR [DESCARTE->DLQ] canal=EMAIL deliveryTag=2 motivo=reintentos agotados
      excepcion=…RecoverableException mensaje=Fallo transitorio simulado del proveedor EMAIL para retry@andesstay.cl
```
Dos reintentos separados 2 s (17:27:56.851 y 17:27:58.867) y descarte inmediatamente después: 3 intentos, como dicta la política.

**D) Estado del broker tras las pruebas**

| Cola | Mensajes | No confirmados |
|------|----------|----------------|
| `q.andesstay.dlq` | **2** | 0 |
| `q.notify.email` / `sms` / `push` | 0 | 0 |
| `q.housekeeping.tickets` / `q.docs.generate` | 0 | 0 |

Ninguna cola de origen conserva mensajes ni entregas sin confirmar: no hay reentrega infinita.

**E) Mensajes en la DLQ** (leídos con `GET /api/queues/andesstay/q.andesstay.dlq/get`)

| # | Origen | Motivo (`x-death`) | Payload |
|---|--------|--------------------|---------|
| 1 | `q.notify.email`, routing key `notify.email` | `rejected` | `{"notificationId":"invalido-1",…,"recipient":"","body":""}` |
| 2 | `q.notify.email`, routing key `notify.email` | `rejected` | `{"notificationId":"183934df-…","reservationId":1003,…,"recipient":"retry@andesstay.cl",…}` |

Los headers `x-death` confirman cola de origen, routing key original y motivo del descarte. Tras capturar la evidencia la DLQ se **purgó** para dejar el broker limpio.

### Rollback
```bash
git revert <hash>
```
Vuelve al ACK automático (seguro) pero se pierde el indicador 5; la cola y el DLX de la tarea 03 quedan intactos.

