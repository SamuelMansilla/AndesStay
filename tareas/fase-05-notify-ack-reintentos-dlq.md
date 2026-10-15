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
- [ ] Los consumidores usan **ACK manual** (`AcknowledgeMode.MANUAL` + `basicAck`).
- [ ] Existe distinción explícita entre error recuperable y no recuperable.
- [ ] Los mensajes que fallan definitivamente llegan a `q.andesstay.dlq`.
- [ ] Todo descarte queda registrado en **logs** con contexto (cola, deliveryTag, motivo).
- [ ] No hay *requeue* infinito.

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
