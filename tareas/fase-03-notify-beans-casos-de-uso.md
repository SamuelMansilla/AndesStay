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
- [ ] Existe al menos un `Queue`, `Exchange` y `Binding` por cada caso de uso definido.
- [ ] Cada ruta de mensajería está **claramente identificada y comentada** en código.
- [ ] Todas las colas tienen dead-lettering configurado hacia DLX/DLQ.
- [ ] No hay literales de nombres (todo desde `MessagingProperties`).
- [ ] `notify` arranca y las 5 colas + DLQ aparecen en la UI de RabbitMQ.

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
